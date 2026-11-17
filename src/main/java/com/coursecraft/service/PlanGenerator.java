package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.dao.SessionDao;
import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.SessionInfo;
import com.coursecraft.model.UnplacedCourse;
import com.coursecraft.util.Db;

public class PlanGenerator {

	public GeneratedPlan preview(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			List<RoadmapCourse> courses = new RoadmapDao().findCoursesForStudent(connection, studentId);
			List<SessionInfo> sessions = new SessionDao().findUnstartedRegularTerms(connection);
			Map<Integer, Double> plannedUnits = new HashMap<>();
			for (PlannedCourse course : new PlanEntryDao().findForStudent(connection, studentId)) {
				double units = plannedUnits.getOrDefault(course.getTermId(), 0.0);
				plannedUnits.put(course.getTermId(), units + course.getUnits());
			}
			return mapCourses(courses, sessions, plannedUnits);
		}
	}

	static GeneratedPlan mapCourses(List<RoadmapCourse> courses, List<SessionInfo> sessions,
			Map<Integer, Double> plannedUnits) {
		GeneratedPlan plan = new GeneratedPlan();
		List<RoadmapCourse> missing = findMissing(courses);
		if (missing.isEmpty()) {
			return plan;
		}
		int earliest = Integer.MAX_VALUE;
		for (RoadmapCourse course : missing) {
			int position = course.getSuggestedTerm();
			if (position > 0 && position < earliest) {
				earliest = position;
			}
		}
		Map<Integer, ProposedTerm> terms = new TreeMap<>();
		for (RoadmapCourse course : missing) {
			// JDBC reads NULL roadmap position as 0.
			if (course.getSuggestedTerm() <= 0) {
				plan.getUnplaced().add(new UnplacedCourse(course.getCourseId(), course.getCode(),
						Double.parseDouble(course.getUnits()), "Roadmap gives " + course.getCode() + " no term."));
				continue;
			}
			// Keeps spacing between roadmap positions, including positions with no missing course.
			int index = course.getSuggestedTerm() - earliest;
			if (index >= sessions.size()) {
				plan.getUnplaced().add(new UnplacedCourse(course.getCourseId(), course.getCode(),
						Double.parseDouble(course.getUnits()), course.getCode() + " has no term available yet."));
				continue;
			}
			SessionInfo session = sessions.get(index);
			ProposedTerm term = terms.get(index);
			if (term == null) {
				term = new ProposedTerm(session.getTermId(), session.getTermLabel(), session.getSeason());
				term.addPlannedUnits(plannedUnits.getOrDefault(session.getTermId(), 0.0));
				terms.put(index, term);
			}
			term.addEntry(new ProposedEntry(course.getCourseId(), course.getCode(), course.getTitle(),
					Double.parseDouble(course.getUnits()), session.getSessionId(), false));
		}
		plan.getTerms().addAll(terms.values());
		return plan;
	}

	private static List<RoadmapCourse> findMissing(List<RoadmapCourse> courses) {
		List<RoadmapCourse> missing = new ArrayList<>();
		for (RoadmapCourse course : courses) {
			boolean required = !"Elective".equals(course.getRequirementType());
			if (required && !course.isPassed() && course.getPlannedTermLabel() == null) {
				missing.add(course);
			}
		}
		return missing;
	}
}
