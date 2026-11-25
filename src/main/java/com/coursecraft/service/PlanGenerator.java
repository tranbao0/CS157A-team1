package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	private static final int SUGGESTION_LIMIT = 12;

	public GeneratedPlan preview(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return preview(connection, studentId);
		}
	}

	public GeneratedPlan preview(Connection connection, int studentId) throws SQLException {
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

	static GeneratedPlan mapCourses(List<RoadmapCourse> courses, List<SessionInfo> sessions,
			List<PlannedCourse> saved, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			Map<Integer, TranscriptCourse> catalog, Map<Integer, String> risks) {
		GeneratedPlan plan = new GeneratedPlan();
		List<RoadmapCourse> pending = findMissing(courses);
		pending.sort(new RoadmapOrder());
		int earliest = earliestPosition(pending);
		Map<Integer, Double> savedUnits = new HashMap<>();
		for (PlannedCourse course : saved) {
			double units = savedUnits.getOrDefault(course.getTermId(), 0.0);
			savedUnits.put(course.getTermId(), units + course.getUnits());
		}
		Placement result = new Placement(pending, earliest, sessions, saved, groups, placements, List.of());
		List<RoadmapCourse> roots = new ArrayList<>();
		for (RoadmapCourse course : pending) {
			int position = course.getSuggestedTerm();
			if (position > 0 && position - earliest < sessions.size()) {
				roots.add(course);
			}
		}
		List<RoadmapCourse> candidates = reachableCourses(courses, roots, saved, groups, placements, catalog);
		List<RoadmapCourse> kept = List.of();
		if (candidates.size() <= SUGGESTION_LIMIT && !candidates.isEmpty()) {
			for (List<RoadmapCourse> chosen : courseSets(candidates)) {
				if (chosen.isEmpty()) {
					continue;
				}
				Placement run = new Placement(pending, earliest, sessions, saved, groups, placements, chosen);
				if (isLegal(run, chosen, saved.size(), groups, placements)
						&& isBetter(run, chosen, result, kept, risks)) {
					result = run;
					kept = chosen;
				}
			}
		}
		// Unplaced reasons describe the plan before the move.
		List<PlannedCourse> beforeMove = new ArrayList<>(result.planned);
		moveHighRiskEarlier(result, sessions, saved.size(), kept, groups, placements, risks);
		int lastOrder = 0;
		if (!sessions.isEmpty()) {
			lastOrder = sessions.get(sessions.size() - 1).getTermOrder();
		}
		PrerequisiteCheck check = new PrerequisiteCheck();
		for (RoadmapCourse course : result.pending) {
			addUnplaced(plan, course, earliest, sessions,
					check.findMissing(course.getCourseId(), lastOrder, groups, beforeMove, placements));
		}
		Comparator<ProposedEntry> displayOrder = new ProposedEntryOrder();
		for (Map.Entry<Integer, List<ProposedEntry>> group : result.entries.entrySet()) {
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

	private static void moveHighRiskEarlier(Placement result, List<SessionInfo> sessions, int savedCount,
			List<RoadmapCourse> kept, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			Map<Integer, String> risks) {
		Set<Integer> moved = new HashSet<>();
		boolean changed = true;
		while (changed) {
			changed = false;
			// Earliest term first, then course code.
			List<ProposedEntry> order = new ArrayList<>();
			for (List<ProposedEntry> term : result.entries.values()) {
				List<ProposedEntry> sorted = new ArrayList<>(term);
				sorted.sort(new ProposedEntryOrder());
				order.addAll(sorted);
			}
			for (ProposedEntry entry : order) {
				int id = entry.getCourseId();
				if (riskOrder(risks.get(id)) == 2 && !moved.contains(id)
						&& moveEarlier(entry, result, sessions, savedCount, kept, groups, placements)) {
					moved.add(id);
					changed = true;
				}
			}
		}
	}

	private static boolean moveEarlier(ProposedEntry entry, Placement result, List<SessionInfo> sessions,
			int savedCount, List<RoadmapCourse> kept, List<PrerequisiteGroup> groups,
			Map<Integer, List<String>> placements) {
		int from = 0;
		for (Map.Entry<Integer, List<ProposedEntry>> term : result.entries.entrySet()) {
			if (term.getValue().contains(entry)) {
				from = term.getKey();
			}
		}
		if (from == 0) {
			return false;
		}
		SessionInfo earlier = sessions.get(from - 1);
		int at = savedCount;
		while (result.planned.get(at).getCourseId() != entry.getCourseId()) {
			at++;
		}
		PlannedCourse course = result.planned.get(at);
		// The course's own row stays out of the check so it cannot meet its own group.
		List<PlannedCourse> others = new ArrayList<>(result.planned);
		others.remove(at);
		if (!hasRoom(course.getUnits(), earlier, others) || !new PrerequisiteCheck()
				.findMissing(course.getCourseId(), earlier.getTermOrder(), groups, others, placements).isEmpty()) {
			return false;
		}
		PlannedCourse moved = atSession(course.getCourseId(), course.getCode(), course.getTitle(),
				String.valueOf(course.getUnits()), earlier);
		List<PlannedCourse> trial = new ArrayList<>(result.planned);
		trial.set(at, moved);
		// A move must leave every Suggested course with a use.
		if (!hasUseForEach(trial, kept, savedCount, groups, placements)) {
			return false;
		}
		result.planned.set(at, moved);
		result.entries.get(from).remove(entry);
		if (result.entries.get(from).isEmpty()) {
			result.entries.remove(from);
		}
		addProposal(result.entries, from - 1, sessions, moved, entry.isSuggested());
		return true;
	}

	private static void placeRoadmap(List<RoadmapCourse> pending, int earliest, List<SessionInfo> sessions,
			List<PlannedCourse> planned, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			Map<Integer, List<ProposedEntry>> entriesByTerm, List<RoadmapCourse> chosen) {
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
			if (next == null) {
				return;
			}
			placeRoadmapCourse(next, index, pending, sessions, planned, entriesByTerm, chosen.contains(next));
		}
	}

	private static void placeRoadmapCourse(RoadmapCourse next, int index, List<RoadmapCourse> pending,
			List<SessionInfo> sessions, List<PlannedCourse> planned, Map<Integer, List<ProposedEntry>> entriesByTerm,
			boolean suggested) {
		pending.remove(next);
		PlannedCourse course = atSession(next.getCourseId(), next.getCode(), next.getTitle(),
				next.getUnits(), sessions.get(index));
		addProposal(entriesByTerm, index, sessions, course, suggested);
		planned.add(course);
	}

	private static List<RoadmapCourse> reachableCourses(List<RoadmapCourse> roadmap, List<RoadmapCourse> roots,
			List<PlannedCourse> saved, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			Map<Integer, TranscriptCourse> catalog) {
		Set<Integer> seen = new LinkedHashSet<>();
		List<RoadmapCourse> owners = new ArrayList<>(roots);
		for (RoadmapCourse course : roadmap) {
			seen.add(course.getCourseId());
		}
		for (PlannedCourse course : saved) {
			seen.add(course.getCourseId());
		}
		List<RoadmapCourse> candidates = new ArrayList<>();
		for (int i = 0; i < owners.size(); i++) {
			int owner = owners.get(i).getCourseId();
			for (PrerequisiteGroup group : groups) {
				if (group.getCourseId() != owner || !placements.getOrDefault(group.getGroupId(), List.of()).isEmpty()) {
					continue;
				}
				for (int id : group.getOptionCourseIds()) {
					TranscriptCourse option = catalog.get(id);
					if (option == null || option.isPassed() || seen.contains(id)) {
						continue;
					}
					seen.add(id);
					String[] code = option.getCode().split(" ", 2);
					RoadmapCourse candidate = new RoadmapCourse(id, code[0], code[1], option.getTitle(),
							option.getUnits(), 1, "Suggested", false, null);
					candidates.add(candidate);
					owners.add(candidate);
				}
			}
		}
		candidates.sort(new RoadmapOrder());
		return candidates;
	}

	private static List<List<RoadmapCourse>> courseSets(List<RoadmapCourse> candidates) {
		List<List<RoadmapCourse>> sets = new ArrayList<>();
		sets.add(List.of());
		for (RoadmapCourse course : candidates) {
			int size = sets.size();
			for (int i = 0; i < size; i++) {
				List<RoadmapCourse> included = new ArrayList<>(sets.get(i));
				included.add(course);
				sets.add(included);
			}
		}
		return sets;
	}

	static boolean isLegal(Placement run, List<RoadmapCourse> chosen, int savedCount,
			List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		return run.valid && hasUseForEach(run.planned, chosen, savedCount, groups, placements);
	}

	private static boolean hasUseForEach(List<PlannedCourse> planned, List<RoadmapCourse> chosen, int savedCount,
			List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		List<PlannedCourse> proposed = planned.subList(savedCount, planned.size());
		for (PlannedCourse course : proposed) {
			for (RoadmapCourse option : chosen) {
				if (course.getCourseId() == option.getCourseId() && !hasUse(course, proposed, planned, groups, placements)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean hasUse(PlannedCourse option, List<PlannedCourse> proposed, List<PlannedCourse> all,
			List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		for (PrerequisiteGroup group : groups) {
			if (!group.hasOption(option.getCourseId()) || !placements.getOrDefault(group.getGroupId(), List.of()).isEmpty()) {
				continue;
			}
			for (PlannedCourse owner : proposed) {
				if (owner.getCourseId() == group.getCourseId() && option.getTermOrder() < owner.getTermOrder()
						&& earlierOptions(group, owner.getTermOrder(), all) == 1) {
					return true;
				}
			}
		}
		return false;
	}

	private static int earlierOptions(PrerequisiteGroup group, int order, List<PlannedCourse> all) {
		int count = 0;
		for (PlannedCourse course : all) {
			if (group.hasOption(course.getCourseId()) && course.getTermOrder() < order) {
				count++;
			}
		}
		return count;
	}

	private static boolean isBetter(Placement run, List<RoadmapCourse> chosen, Placement best,
			List<RoadmapCourse> kept, Map<Integer, String> risks) {
		if (run.remaining() != best.remaining()) {
			return run.remaining() < best.remaining();
		}
		if (chosen.size() != kept.size()) {
			return chosen.size() < kept.size();
		}
		int risk = highestRisk(chosen, risks);
		int bestRisk = highestRisk(kept, risks);
		if (risk != bestRisk) {
			return risk < bestRisk;
		}
		CourseCodeOrder order = new CourseCodeOrder();
		for (int i = 0; i < chosen.size(); i++) {
			int comparison = order.compare(chosen.get(i).getCode(), kept.get(i).getCode());
			if (comparison != 0) {
				return comparison < 0;
			}
		}
		return false;
	}

	private static int highestRisk(List<RoadmapCourse> chosen, Map<Integer, String> risks) {
		int highest = 0;
		for (RoadmapCourse course : chosen) {
			String risk = risks.get(course.getCourseId());
			highest = Math.max(highest, riskOrder(risk));
		}
		return highest;
	}

	private static int earliestPosition(List<RoadmapCourse> pending) {
		for (RoadmapCourse course : pending) {
			int position = course.getSuggestedTerm();
			if (position > 0) {
				return position;
			}
		}
		return Integer.MAX_VALUE;
	}

	static class Placement {
		private final List<RoadmapCourse> pending;
		private final List<PlannedCourse> planned;
		private final Map<Integer, List<ProposedEntry>> entries = new TreeMap<>();
		final boolean valid;

		Placement(List<RoadmapCourse> courses, int earliest, List<SessionInfo> sessions,
				List<PlannedCourse> saved, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
				List<RoadmapCourse> chosen) {
			pending = new ArrayList<>(courses);
			pending.addAll(chosen);
			pending.sort(new RoadmapOrder());
			planned = new ArrayList<>(saved);
			placeRoadmap(pending, earliest, sessions, planned, groups, placements, entries, chosen);
			valid = Collections.disjoint(pending, chosen) && prerequisitesMet(saved.size(), groups, placements);
			pending.removeAll(chosen);
		}

		int remaining() {
			return pending.size();
		}

		List<PlannedCourse> plannedCourses() {
			return List.copyOf(planned);
		}

		private boolean prerequisitesMet(int first, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
			PrerequisiteCheck check = new PrerequisiteCheck();
			for (int i = first; i < planned.size(); i++) {
				PlannedCourse course = planned.get(i);
				List<String> missing = check.findMissing(course.getCourseId(), course.getTermOrder(), groups, planned,
						placements);
				if (!missing.isEmpty()) {
					return false;
				}
			}
			return true;
		}
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
			for (int index = Math.max(0, position - earliest); index < sessions.size(); index++) {
				SessionInfo term = sessions.get(index);
				if (hasRoom(course, term, planned)
						&& check.findMissing(course.getCourseId(), term.getTermOrder(), groups, planned, placements).isEmpty()) {
					return index;
				}
			}
		}
		return sessions.size();
	}

	private static boolean hasRoom(RoadmapCourse course, SessionInfo term, List<PlannedCourse> planned) {
		return hasRoom(Double.parseDouble(course.getUnits()), term, planned);
	}

	private static boolean hasRoom(double courseUnits, SessionInfo term, List<PlannedCourse> planned) {
		double units = courseUnits;
		for (PlannedCourse entry : planned) {
			if (entry.getTermId() == term.getTermId()) {
				units += entry.getUnits();
			}
		}
		return units <= UnitRules.NORMAL_LIMIT;
	}

	private static void addUnplaced(GeneratedPlan plan, RoadmapCourse course, int earliest,
			List<SessionInfo> sessions, List<String> missing) {
		String reason = course.getCode() + " has no term available yet.";
		// JDBC reads NULL roadmap position as 0.
		if (course.getSuggestedTerm() <= 0) {
			reason = "Roadmap gives " + course.getCode() + " no term.";
		} else if (course.getSuggestedTerm() - earliest < sessions.size()) {
			if (missing.isEmpty()) {
				SessionInfo lastTerm = sessions.get(sessions.size() - 1);
				reason = "No room under " + UnitRules.format(UnitRules.NORMAL_LIMIT) + " units by "
						+ lastTerm.getTermLabel() + ".";
			} else {
				reason = "Needs " + PrerequisiteCheck.joinMissing(missing) + " first.";
			}
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
