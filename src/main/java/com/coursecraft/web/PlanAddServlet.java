package com.coursecraft.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.coursecraft.util.Db;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/plan/add")
public class PlanAddServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Session & Auth Validation
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("studentId") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must be logged in to modify plans.");
            return;
        }

        int studentId = (Integer) session.getAttribute("studentId");
        int courseId;
        int sessionId;

        try {
            courseId = Integer.parseInt(request.getParameter("courseId"));
            sessionId = Integer.parseInt(request.getParameter("sessionId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid courseId or sessionId.");
            return;
        }

        try (Connection conn = Db.getConnection()) {

            // 409 Conflict Check: Already in current plan session or already completed
            if (isAlreadyPlannedOrPassed(conn, studentId, courseId, sessionId)) {
                response.sendError(HttpServletResponse.SC_CONFLICT, "Course is already planned for this session or already completed.");
                return;
            }

            // 422 Unprocessable Entity Check: Prerequisite Validation
            if (!hasMetAllPrerequisites(conn, studentId, courseId)) {
                response.sendError(422, "Prerequisites not satisfied for this course.");
                return;
            }

            // Insert into plan_entries on success
            String insertSql = "INSERT INTO plan_entries (student_id, course_id, session_id, added_at) VALUES (?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setInt(1, studentId);
                stmt.setInt(2, courseId);
                stmt.setInt(3, sessionId);
                stmt.executeUpdate();
            }

            // 201 Created
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write("{\"status\":\"success\",\"message\":\"Course added to plan.\"}");

        } catch (SQLException e) {
            getServletContext().log("Database error processing /plan/add", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database transaction failure.");
        }
    }

    private boolean isAlreadyPlannedOrPassed(Connection conn, int studentId, int courseId, int sessionId) throws SQLException {
        String query = 
            "SELECT 1 FROM plan_entries WHERE student_id = ? AND course_id = ? AND session_id = ? " +
            "UNION " +
            "SELECT 1 FROM completed WHERE student_id = ? AND course_id = ? AND taken_and_passed = 1";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            stmt.setInt(3, sessionId);
            stmt.setInt(4, studentId);
            stmt.setInt(5, courseId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasMetAllPrerequisites(Connection conn, int studentId, int courseId) throws SQLException {
        // Find any prerequisite group for this course where the student has NOT passed a qualifying course
        String query = 
            "SELECT pg.group_id " +
            "FROM prerequisite_groups pg " +
            "WHERE pg.course_id = ? " +
            "  AND NOT EXISTS (" +
            "      SELECT 1 " +
            "      FROM counts_towards ct, completed c " +
            "      WHERE ct.group_id = pg.group_id " +
            "        AND ct.course_id = c.course_id " +
            "        AND c.student_id = ? " +
            "        AND c.taken_and_passed = 1" +
            "  )";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, courseId);
            stmt.setInt(2, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                // If any group exists without fulfillment, prerequisites are NOT met
                return !rs.next();
            }
        }
    }
}