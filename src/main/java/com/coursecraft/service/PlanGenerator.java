package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.PrerequisiteDao;
import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.dao.SessionDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;
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
			PrerequisiteDao prerequisites = new PrerequisiteDao();
			int year = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
			GeneratedPlan plan = mapCourses(courses, sessions, new PlanEntryDao().findForStudent(connection, studentId),
					prerequisites.findOpenGroups(connection, studentId, year), prerequisites.findPlacementOptions(connection));
			Integer gradTermId = new StudentDao().findProfile(connection, studentId).getGradTermId();
			for (ProposedTerm term : plan.getTerms()) {
				if (gradTermId != null && gradTermId == term.getTermId()) {
					term.markGradTerm();
				}
			}
			return plan;
		}
	}

	static GeneratedPlan mapCourses(List<RoadmapCourse> courses, List<SessionInfo> sessions,
			List<PlannedCourse> saved, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		GeneratedPlan plan = new GeneratedPlan();
		List<RoadmapCourse> pending = findMissing(courses);
		pending.sort(new RoadmapOrder());
		int earliest = Integer.MAX_VALUE;
		Map<Integer, Double> savedUnits = new HashMap<>();
		for (PlannedCourse course : saved) {
			double units = savedUnits.getOrDefault(course.getTermId(), 0.0);
			savedUnits.put(course.getTermId(), units + course.getUnits());
		}
		for (RoadmapCourse course : pending) {
			if (course.getSuggestedTerm() > 0) {
				earliest = course.getSuggestedTerm();
				break;
			}
		}
		List<PlannedCourse> planned = new ArrayList<>(saved);
		Map<Integer, List<ProposedEntry>> entriesByTerm = new TreeMap<>();
		PrerequisiteCheck check = new PrerequisiteCheck();
		while (!pending.isEmpty()) {
			RoadmapCourse next = null;
			for (RoadmapCourse course : pending) {
				if (!hasPendingOption(course, pending, groups)) {
					next = course;
					break;
				}
			}
			if (next == null) {
				// An earlier OR choice can let a course leave a cycle.
				for (RoadmapCourse course : pending) {
					if (findTerm(course, earliest, sessions, planned, groups, placements) < sessions.size()) {
						next = course;
						break;
					}
				}
			}
			if (next == null) {
				for (RoadmapCourse course : pending) {
					addUnplaced(plan, course, earliest, sessions.size(),
							check.findMissing(course.getCourseId(), Integer.MAX_VALUE, groups, planned, placements));
				}
				break;
			}
			pending.remove(next);
			int index = findTerm(next, earliest, sessions, planned, groups, placements);
			if (index == sessions.size()) {
				addUnplaced(plan, next, earliest, sessions.size(),
						check.findMissing(next.getCourseId(), Integer.MAX_VALUE, groups, planned, placements));
				continue;
			}
			SessionInfo session = sessions.get(index);
			List<ProposedEntry> entries = entriesByTerm.get(index);
			if (entries == null) {
				entries = new ArrayList<>();
				entriesByTerm.put(index, entries);
			}
			double units = Double.parseDouble(next.getUnits());
			entries.add(new ProposedEntry(next.getCourseId(), next.getCode(), next.getTitle(),
					units, session.getSessionId(), false));
			planned.add(new PlannedCourse(0, next.getCourseId(), next.getCode(), next.getTitle(), units,
					session.getTermId(), session.getTermLabel(), session.getSeason(), session.getTermOrder(), null));
		}
		Comparator<ProposedEntry> displayOrder = new ProposedEntryOrder();
		for (Map.Entry<Integer, List<ProposedEntry>> group : entriesByTerm.entrySet()) {
			SessionInfo session = sessions.get(group.getKey());
			ProposedTerm term = new ProposedTerm(session.getTermId(), session.getTermLabel(), session.getSeason(),
					session.getTermOrder());
			term.addPlannedUnits(savedUnits.getOrDefault(session.getTermId(), 0.0));
			List<ProposedEntry> entries = group.getValue();
			entries.sort(displayOrder);
			for (ProposedEntry entry : entries) {
				term.addEntry(entry);
			}
			plan.getTerms().add(term);
		}
		return plan;
	}

	private static boolean hasPendingOption(RoadmapCourse course, List<RoadmapCourse> pending,
			List<PrerequisiteGroup> groups) {
		for (PrerequisiteGroup group : groups) {
			if (group.getCourseId() == course.getCourseId()) {
				for (RoadmapCourse option : pending) {
					if (group.hasOption(option.getCourseId())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static int findTerm(RoadmapCourse course, int earliest, List<SessionInfo> sessions,
			List<PlannedCourse> planned, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		// JDBC reads NULL roadmap position as 0.
		int position = course.getSuggestedTerm();
		if (position > 0) {
			PrerequisiteCheck check = new PrerequisiteCheck();
			for (int index = position - earliest; index < sessions.size(); index++) {
				if (check.findMissing(course.getCourseId(), sessions.get(index).getTermOrder(),
						groups, planned, placements).isEmpty()) {
					return index;
				}
			}
		}
		return sessions.size();
	}

	private static void addUnplaced(GeneratedPlan plan, RoadmapCourse course, int earliest,
			int termCount, List<String> missing) {
		String reason = course.getCode() + " has no term available yet.";
		// JDBC reads NULL roadmap position as 0.
		if (course.getSuggestedTerm() <= 0) {
			reason = "Roadmap gives " + course.getCode() + " no term.";
		} else if (course.getSuggestedTerm() - earliest < termCount && !missing.isEmpty()) {
			reason = "Needs " + String.join(", and ", missing) + " first.";
		}
		plan.getUnplaced().add(new UnplacedCourse(course.getCourseId(), course.getCode(),
				Double.parseDouble(course.getUnits()), reason));
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
	private static class RoadmapOrder implements Comparator<RoadmapCourse> {
		private final CourseCodeOrder codeOrder = new CourseCodeOrder();

		@Override
		public int compare(RoadmapCourse first, RoadmapCourse second) {
			int positionOrder = Integer.compare(first.getSuggestedTerm(), second.getSuggestedTerm());
			if (positionOrder != 0) {
				return positionOrder;
			}
			return codeOrder.compare(first.getCode(), second.getCode());
		}
	}

	private static class ProposedEntryOrder implements Comparator<ProposedEntry> {
		private final CourseCodeOrder codeOrder = new CourseCodeOrder();

		@Override
		public int compare(ProposedEntry first, ProposedEntry second) {
			return codeOrder.compare(first.getCode(), second.getCode());
		}
	}
}
