package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.CompletedDao;
import com.coursecraft.dao.SessionDao;
import com.coursecraft.dao.PrerequisiteDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;
import com.coursecraft.util.Db;

// One request reads the student's evidence once, regardless of the number of sections.
public class PlanAvailability {
    private boolean knownStudent;
    private List<PrerequisiteGroup> groups = List.of();
    private List<PlannedCourse> planned = List.of();
    private Map<Integer, List<String>> placements = Map.of();
    private final Set<Integer> completed = new HashSet<>();
    private final Set<Integer> inPlan = new HashSet<>();
    private final Map<Integer, Integer> sessionTerms = new HashMap<>();
    private final Map<String, String> reasons = new HashMap<>();

    public static PlanAvailability read(int studentId) throws SQLException {
        PlanAvailability result = new PlanAvailability();
        try (Connection connection = Db.getConnection()) {
            connection.setAutoCommit(false);
            try {
                result.load(connection, studentId);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        return result;
    }

    private void load(Connection connection, int studentId) throws SQLException {
        knownStudent = new StudentDao().exists(connection, studentId);
        if (!knownStudent) {
            return;
        }
        int year = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
        PrerequisiteDao dao = new PrerequisiteDao();
        groups = dao.findOpenGroups(connection, studentId, year);
        placements = dao.findPlacementOptions(connection);
        planned = new PlanEntryDao().findForStudent(connection, studentId);
        for (PlannedCourse course : planned) {
            inPlan.add(course.getCourseId());
        }
        completed.addAll(new CompletedDao().findPassedIds(connection, studentId));
        sessionTerms.putAll(new SessionDao().findOpenTermOrders(connection));
    }

    public boolean hasStudent() {
        return knownStudent;
    }

    public boolean isCompleted(int courseId) {
        return completed.contains(courseId);
    }

    public boolean isInPlan(int courseId) {
        return inPlan.contains(courseId);
    }

    // A missing snapshot or term never supplies evidence that prerequisites are met.
    public String reason(int courseId, int sessionId) {
        if (!knownStudent) {
            return "No student account is available. Sign in again.";
        }
        if (!sessionTerms.containsKey(sessionId)) {
            return "No current or future session is available.";
        }
        String key = courseId + ":" + sessionId;
        if (!reasons.containsKey(key)) {
            List<String> missing = new PrerequisiteCheck().findMissing(courseId, sessionTerms.get(sessionId),
                    groups, planned, placements);
            String reason = "";
            if (!missing.isEmpty()) {
                reason = "Needs " + String.join(", and ", missing) + " first.";
            }
            reasons.put(key, reason);
        }
        return reasons.get(key);
    }
}
