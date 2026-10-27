package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.PlanEntry;
import com.coursecraft.model.PlannedCourse;

// SQL for plan_entries table lives here, servlets just call these methods
// every method takes caller's connection, so plan add runs all of them in one transaction
public class PlanEntryDao {

	// finds session where student already planned this course, null when course is not in plan yet
	public Integer findPlannedSessionId(Connection connection, int studentId, int courseId) throws SQLException {
		String sql = "SELECT session_id FROM plan_entries WHERE student_id = ? AND course_id = ? LIMIT 1";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setInt(2, courseId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getInt("session_id");
			}
		}
	}

	// adds up units student planned in one term, all sessions of that term count, 0 when none
	public double sumUnitsInTerm(Connection connection, int studentId, int termId) throws SQLException {
		String sql = "SELECT COALESCE(SUM(c.units), 0) AS units "
				+ "FROM plan_entries pe "
				+ "JOIN courses c ON c.course_id = pe.course_id "
				+ "JOIN sessions s ON s.session_id = pe.session_id "
				+ "WHERE pe.student_id = ? AND s.term_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setInt(2, termId);

			try (ResultSet rs = statement.executeQuery()) {
				rs.next();
				return rs.getDouble("units");
			}
		}
	}

	/*
	 * Lists every course in student's plan with its term and risk level, oldest term first.
	 * Term order is year * 10 + season place (Winter 1, Spring 2, Summer 3, Fall 4),
	 * so Fall 2026 = 20264 comes before Winter 2027 = 20271. PrerequisiteCheck compares terms with it.
	 */
	public List<PlannedCourse> findForStudent(Connection connection, int studentId) throws SQLException {
		String sql = "SELECT pe.plan_entry_id, pe.course_id, CONCAT(c.subject, ' ', c.course_number) AS code, "
				+ "       c.title, c.units, t.term_id, CONCAT(t.season, ' ', t.year) AS term_label, t.season, "
				+ "       t.year * 10 + FIELD(t.season, 'Winter', 'Spring', 'Summer', 'Fall') AS term_order, "
				+ "       (SELECT b.risk_level FROM bottleneck_scores b "
				+ "        WHERE b.course_id = pe.course_id AND b.term_id = t.term_id "
				+ "        ORDER BY b.computed_at DESC LIMIT 1) AS risk_level "
				+ "FROM plan_entries pe "
				+ "JOIN courses c ON c.course_id = pe.course_id "
				+ "JOIN sessions s ON s.session_id = pe.session_id "
				+ "JOIN terms t ON t.term_id = s.term_id "
				+ "WHERE pe.student_id = ? "
				+ "ORDER BY term_order, c.subject, c.course_number";

		List<PlannedCourse> courses = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					courses.add(new PlannedCourse(
							rs.getInt("plan_entry_id"),
							rs.getInt("course_id"),
							rs.getString("code"),
							rs.getString("title"),
							rs.getDouble("units"),
							rs.getInt("term_id"),
							rs.getString("term_label"),
							rs.getString("season"),
							rs.getInt("term_order"),
							rs.getString("risk_level")));
				}
			}
		}
		return courses;
	}

	// finds one plan row only when it belongs to this student, so nobody can touch other student's plan
	public PlanEntry findEntry(Connection connection, int studentId, int planEntryId) throws SQLException {
		String sql = "SELECT plan_entry_id, course_id, session_id FROM plan_entries "
				+ "WHERE plan_entry_id = ? AND student_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, planEntryId);
			statement.setInt(2, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new PlanEntry(rs.getInt("plan_entry_id"), rs.getInt("course_id"), rs.getInt("session_id"));
			}
		}
	}

	// deletes one plan row, student_id in WHERE keeps delete inside student's own plan
	public void delete(Connection connection, int studentId, int planEntryId) throws SQLException {
		String sql = "DELETE FROM plan_entries WHERE plan_entry_id = ? AND student_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, planEntryId);
			statement.setInt(2, studentId);
			statement.executeUpdate();
		}
	}

	// adds one course to student's plan for one session, MySQL picks plan_entry_id
	public void add(Connection connection, int studentId, int courseId, int sessionId) throws SQLException {
		String sql = "INSERT INTO plan_entries (student_id, course_id, session_id, added_at) "
				+ "VALUES (?, ?, ?, NOW())";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setInt(2, courseId);
			statement.setInt(3, sessionId);
			statement.executeUpdate();
		}
	}
}
