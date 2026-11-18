package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.coursecraft.dao.BottleneckScoreDao;
import com.coursecraft.dao.CourseDao;
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
import com.coursecraft.model.TranscriptCourse;
import com.coursecraft.model.UnplacedCourse;
import com.coursecraft.util.Db;

public class PlanGenerator {

	public GeneratedPlan preview(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			List<RoadmapCourse> courses = new RoadmapDao().findCoursesForStudent(connection, studentId);
			List<SessionInfo> sessions = new SessionDao().findUnstartedRegularTerms(connection);
			PrerequisiteDao prerequisites = new PrerequisiteDao();
			int year = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
			Map<Integer, TranscriptCourse> catalog = new HashMap<>();
			for (TranscriptCourse course : new CourseDao().findForTranscript(connection, studentId)) {
				catalog.put(course.getCourseId(), course);
			}
			GeneratedPlan plan = mapCourses(courses, sessions, new PlanEntryDao().findForStudent(connection, studentId),
					prerequisites.findOpenGroups(connection, studentId, year), prerequisites.findPlacementOptions(connection),
					catalog, new BottleneckScoreDao().findNewestRisks(connection));
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
			List<PlannedCourse> saved, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			Map<Integer, TranscriptCourse> catalog, Map<List<Integer>, String> risks) {
		GeneratedPlan plan = new GeneratedPlan();
		List<RoadmapCourse> pending = findMissing(courses);
		pending.sort(new RoadmapOrder());
		Map<Integer, TranscriptCourse> candidates = new HashMap<>(catalog);
		for (RoadmapCourse course : pending) {
			candidates.remove(course.getCourseId());
		}
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
			int index = sessions.size();
			for (RoadmapCourse course : pending) {
				int target = findTerm(course, earliest, sessions, planned, groups, placements);
				if (target == sessions.size()) {
					continue;
				}
				if (!hasPendingOption(course, pending, groups)) {
					next = course;
					index = target;
					break;
				}
				if (next == null) {
					next = course;
					index = target;
				}
			}
			// Try dependents without pending roadmap options before allowing a cycle escape.
			for (int pass = 0; pass < 2 && next == null; pass++) {
				for (RoadmapCourse course : pending) {
					int position = course.getSuggestedTerm();
					boolean waitsForRoadmap = hasPendingOption(course, pending, groups);
					if (position <= 0 || position - earliest >= sessions.size()
							|| waitsForRoadmap != (pass == 1)) {
						continue;
					}
					index = suggestFor(course, earliest, sessions, planned, groups, placements,
							candidates, risks, entriesByTerm);
					if (index < sessions.size()) {
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
			PlannedCourse course = atSession(next.getCourseId(), next.getCode(), next.getTitle(),
					next.getUnits(), sessions.get(index));
			addProposal(entriesByTerm, index, sessions, course, false);
			planned.add(course);
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

	private static int suggestFor(RoadmapCourse dependent, int earliest, List<SessionInfo> sessions,
			List<PlannedCourse> planned, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			Map<Integer, TranscriptCourse> candidates, Map<List<Integer>, String> risks,
			Map<Integer, List<ProposedEntry>> entries) {
		PrerequisiteCheck check = new PrerequisiteCheck();
		List<PlannedCourse> trial = new ArrayList<>(planned);
		Map<PlannedCourse, Integer> picked = new LinkedHashMap<>();
		for (PrerequisiteGroup group : groups) {
			if (group.getCourseId() != dependent.getCourseId()
					|| check.findMissing(dependent.getCourseId(), Integer.MAX_VALUE,
							List.of(group), trial, placements).isEmpty()) {
				continue;
			}
			List<String> acceptedPlacements = placements.getOrDefault(group.getGroupId(), List.of());
			if (!acceptedPlacements.isEmpty()) {
				return sessions.size();
			}
			TranscriptCourse best = null;
			int bestTerm = -1;
			int bestRisk = 0;
			CourseCodeOrder codeOrder = new CourseCodeOrder();
			for (int option : group.getOptionCourseIds()) {
				TranscriptCourse course = candidates.get(option);
				if (course == null || course.isPassed()) {
					continue;
				}
				// Leave at least one term for dependent and check options against courses already placed.
				for (int i = 0; i < sessions.size() - 1; i++) {
					SessionInfo session = sessions.get(i);
					if (!check.findMissing(option, session.getTermOrder(), groups, planned, placements).isEmpty()) {
						continue;
					}
					int risk = riskOrder(risks.get(List.of(session.getTermId(), option)));
					boolean better = false;
					if (best == null || risk < bestRisk) {
						better = true;
					} else if (risk == bestRisk && codeOrder.compare(course.getCode(), best.getCode()) < 0) {
						better = true;
					}
					if (better) {
						best = course;
						bestTerm = i;
						bestRisk = risk;
					}
					break;
				}
			}
			if (best == null) {
				return sessions.size();
			}
			PlannedCourse choice = atSession(best.getCourseId(), best.getCode(), best.getTitle(),
					best.getUnits(), sessions.get(bestTerm));
			trial.add(choice);
			picked.put(choice, bestTerm);
		}
		int index = findTerm(dependent, earliest, sessions, trial, groups, placements);
		if (index < sessions.size()) {
			for (Map.Entry<PlannedCourse, Integer> choice : picked.entrySet()) {
				addProposal(entries, choice.getValue(), sessions, choice.getKey(), true);
				planned.add(choice.getKey());
			}
		}
		return index;
	}

	private static int riskOrder(String risk) {
		if ("High".equals(risk)) {
			return 2;
		}
		if ("Medium".equals(risk)) {
			return 1;
		}
		return 0;
	}

	private static PlannedCourse atSession(int id, String code, String title, String units, SessionInfo session) {
		return new PlannedCourse(0, id, code, title, Double.parseDouble(units), session.getTermId(),
				session.getTermLabel(), session.getSeason(), session.getTermOrder(), null);
	}

	private static void addProposal(Map<Integer, List<ProposedEntry>> entries, int index,
			List<SessionInfo> sessions, PlannedCourse course, boolean suggested) {
		List<ProposedEntry> term = entries.get(index);
		if (term == null) {
			term = new ArrayList<>();
			entries.put(index, term);
		}
		term.add(new ProposedEntry(course.getCourseId(), course.getCode(), course.getTitle(),
				course.getUnits(), sessions.get(index).getSessionId(), suggested));
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
			reason = "Needs " + PrerequisiteCheck.joinMissing(missing) + " first.";
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
