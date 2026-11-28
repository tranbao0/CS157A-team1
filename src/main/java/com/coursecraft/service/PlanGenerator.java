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
import com.coursecraft.dao.GeRuleDao;
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
import com.coursecraft.model.RoadmapGeStatus;
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
		RoadmapDao roadmap = new RoadmapDao();
		List<RoadmapCourse> courses = roadmap.findCoursesForStudent(connection, studentId);
		List<SessionInfo> sessions = new SessionDao().findUnstartedRegularTerms(connection);
		PrerequisiteDao prerequisites = new PrerequisiteDao();
		int year = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
		Map<Integer, TranscriptCourse> catalog = new HashMap<>();
		for (TranscriptCourse course : new CourseDao().findForTranscript(connection, studentId)) {
			catalog.put(course.getCourseId(), course);
		}
		UpperDivisionGeRule udRule = new GeRuleDao().load(connection, studentId);
		GeneratedPlan plan = mapCourses(courses, sessions, new PlanEntryDao().findForStudent(connection, studentId),
				prerequisites.findOpenGroups(connection, studentId, year), prerequisites.findPlacementOptions(connection),
				udRule, catalog, new BottleneckScoreDao().findNewestRisks(connection),
				roadmap.findGeAreasForStudent(connection, studentId));
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
			UpperDivisionGeRule udRule, Map<Integer, TranscriptCourse> catalog, Map<Integer, String> risks,
			List<RoadmapGeStatus> geAreas) {
		GeneratedPlan plan = new GeneratedPlan();
		List<RoadmapCourse> pending = findMissing(courses);
		pending.sort(new RoadmapOrder());
		int earliest = earliestPosition(pending);
		Map<Integer, Double> savedUnits = new HashMap<>();
		for (PlannedCourse course : saved) {
			double units = savedUnits.getOrDefault(course.getTermId(), 0.0);
			savedUnits.put(course.getTermId(), units + course.getUnits());
		}
		Placement result = new Placement(pending, earliest, sessions, saved, groups, placements, udRule, List.of());
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
				Placement run = new Placement(pending, earliest, sessions, saved, groups, placements, udRule, chosen);
				if (isLegal(run, chosen, saved.size(), groups, placements)
						&& isBetter(run, chosen, result, kept, risks)) {
					result = run;
					kept = chosen;
				}
			}
		}
		moveHighRiskEarlier(result, sessions, saved.size(), kept, groups, placements, udRule, risks);
		finishPlacement(plan, result, earliest, geAreas, sessions, groups, placements, udRule, catalog, risks);
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

	private static void finishPlacement(GeneratedPlan plan, Placement result, int earliest,
			List<RoadmapGeStatus> geAreas, List<SessionInfo> sessions, List<PrerequisiteGroup> groups,
			Map<Integer, List<String>> placements, UpperDivisionGeRule udRule, Map<Integer, TranscriptCourse> catalog,
			Map<Integer, String> risks) {
		Set<String> covered = new HashSet<>();
		for (PlannedCourse row : result.planned) {
			coverGeAreas(covered, catalog.get(row.getCourseId()));
		}
		Set<Integer> roadmapIds = new HashSet<>();
		Set<String> reserved = new HashSet<>();
		for (RoadmapCourse course : result.pending) {
			roadmapIds.add(course.getCourseId());
			coverGeAreas(reserved, catalog.get(course.getCourseId()));
		}
		Map<String, List<TranscriptCourse>> optionsByArea = new TreeMap<>();
		for (RoadmapGeStatus area : geAreas) {
			String code = area.getCode();
			if (!code.startsWith("UD") && !area.isFulfilled() && !covered.contains(code) && !reserved.contains(code)) {
				optionsByArea.put(code, new ArrayList<>());
			}
		}
		for (TranscriptCourse course : catalog.values()) {
			if (course.getGeAreaCode() == null) {
				continue;
			}
			List<TranscriptCourse> options = optionsByArea.get(course.getGeAreaCode());
			if (options != null && !course.isPassed() && !roadmapIds.contains(course.getCourseId())) {
				options.add(course);
			}
		}
		GePickOrder order = new GePickOrder(risks);
		for (List<TranscriptCourse> options : optionsByArea.values()) {
			options.sort(order);
		}
		int previousSize;
		do {
			previousSize = result.planned.size();
			pickOpenAreas(result, optionsByArea, covered, sessions, groups, placements, udRule);
			int waitingCount;
			do {
				waitingCount = result.pending.size();
				for (RoadmapCourse course : new ArrayList<>(result.pending)) {
					int index = findTerm(course, earliest, sessions, result.planned, groups, placements, udRule);
					if (index < sessions.size()) {
						placeRoadmapCourse(course, index, result.pending, sessions, result.planned, result.entries, false);
						coverGeAreas(covered, catalog.get(course.getCourseId()));
					}
				}
			} while (result.pending.size() < waitingCount);
		} while (result.planned.size() > previousSize);

		for (RoadmapCourse course : result.pending) {
			String reason = course.getCode() + " has no term available yet.";
			if (course.getSuggestedTerm() <= 0) {
				reason = "Roadmap gives " + course.getCode() + " no term.";
			} else if (course.getSuggestedTerm() - earliest < sessions.size()) {
				reason = finalUnplacedReason(course.getCourseId(), sessions.get(sessions.size() - 1),
						groups, result.planned, placements, udRule);
			}
			plan.getUnplaced().add(new UnplacedCourse(course.getCourseId(), course.getCode(),
					Double.parseDouble(course.getUnits()), reason));
		}
		for (Map.Entry<String, List<TranscriptCourse>> area : optionsByArea.entrySet()) {
			if (covered.contains(area.getKey()) || area.getValue().isEmpty()) {
				continue;
			}
			TranscriptCourse top = area.getValue().get(0);
			String reason = top.getCode() + " has no term available yet.";
			if (!sessions.isEmpty()) {
				reason = finalUnplacedReason(top.getCourseId(), sessions.get(sessions.size() - 1),
						groups, result.planned, placements, udRule);
			}
			plan.getUnplaced().add(new UnplacedCourse(top.getCourseId(), top.getCode(),
					Double.parseDouble(top.getUnits()), reason));
		}
	}

	private static void pickOpenAreas(Placement result, Map<String, List<TranscriptCourse>> optionsByArea,
			Set<String> covered, List<SessionInfo> sessions, List<PrerequisiteGroup> groups,
			Map<Integer, List<String>> placements, UpperDivisionGeRule udRule) {
		for (Map.Entry<String, List<TranscriptCourse>> area : optionsByArea.entrySet()) {
			if (covered.contains(area.getKey())) {
				continue;
			}
			for (TranscriptCourse course : area.getValue()) {
				int index = findTerm(course.getCourseId(), Double.parseDouble(course.getUnits()), 0,
						sessions, result.planned, groups, placements, udRule);
				if (index < sessions.size()) {
					PlannedCourse row = atSession(course.getCourseId(), course.getCode(), course.getTitle(),
							course.getUnits(), sessions.get(index));
					result.planned.add(row);
					addProposal(result.entries, index, sessions, row, true);
					coverGeAreas(covered, course);
					break;
				}
			}
		}
	}

	private static String finalUnplacedReason(int courseId, SessionInfo lastTerm, List<PrerequisiteGroup> groups,
			List<PlannedCourse> planned, Map<Integer, List<String>> placements, UpperDivisionGeRule udRule) {
		List<String> missing = missingPrerequisites(courseId, Integer.MAX_VALUE, groups, planned, placements, udRule);
		if (!missing.isEmpty()) {
			return "Needs " + PrerequisiteCheck.joinMissing(missing) + " first.";
		}
		if (!missingPrerequisites(courseId, lastTerm.getTermOrder(), groups, planned, placements, udRule).isEmpty()) {
			return "Prerequisites finish too late for the available terms.";
		}
		return "No room under " + UnitRules.format(UnitRules.NORMAL_LIMIT) + " units by "
				+ lastTerm.getTermLabel() + ".";
	}

	private static void coverGeAreas(Set<String> covered, TranscriptCourse course) {
		if (course == null || course.getGeAreaCode() == null) {
			return;
		}
		String area = course.getGeAreaCode();
		covered.add(area);
		if (("5A".equals(area) || "5B".equals(area)) && Double.parseDouble(course.getUnits()) >= 4) {
			covered.add("5C");
		}
	}

	private static void moveHighRiskEarlier(Placement result, List<SessionInfo> sessions, int savedCount,
			List<RoadmapCourse> kept, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			UpperDivisionGeRule udRule, Map<Integer, String> risks) {
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
						&& moveEarlier(entry, result, sessions, savedCount, kept, groups, placements, udRule)) {
					moved.add(id);
					changed = true;
				}
			}
		}
	}

	private static boolean moveEarlier(ProposedEntry entry, Placement result, List<SessionInfo> sessions,
			int savedCount, List<RoadmapCourse> kept, List<PrerequisiteGroup> groups,
			Map<Integer, List<String>> placements, UpperDivisionGeRule udRule) {
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
		if (!hasRoom(course.getUnits(), earlier, others)
				|| !missingPrerequisites(course.getCourseId(), earlier.getTermOrder(), groups, others, placements,
						udRule).isEmpty()) {
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
			UpperDivisionGeRule udRule, Map<Integer, List<ProposedEntry>> entriesByTerm, List<RoadmapCourse> chosen) {
		while (!pending.isEmpty()) {
			RoadmapCourse next = null;
			int index = sessions.size();
			for (RoadmapCourse course : pending) {
				int target = findTerm(course, earliest, sessions, planned, groups, placements, udRule);
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
				UpperDivisionGeRule udRule, List<RoadmapCourse> chosen) {
			pending = new ArrayList<>(courses);
			pending.addAll(chosen);
			pending.sort(new RoadmapOrder());
			planned = new ArrayList<>(saved);
			placeRoadmap(pending, earliest, sessions, planned, groups, placements, udRule, entries, chosen);
			valid = Collections.disjoint(pending, chosen) && prerequisitesMet(saved.size(), groups, placements, udRule);
			pending.removeAll(chosen);
		}

		int remaining() {
			return pending.size();
		}

		List<PlannedCourse> plannedCourses() {
			return List.copyOf(planned);
		}

		private boolean prerequisitesMet(int first, List<PrerequisiteGroup> groups,
				Map<Integer, List<String>> placements, UpperDivisionGeRule udRule) {
			for (int i = first; i < planned.size(); i++) {
				PlannedCourse course = planned.get(i);
				List<String> missing = missingPrerequisites(course.getCourseId(), course.getTermOrder(), groups, planned,
						placements, udRule);
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

	private static List<String> missingPrerequisites(int courseId, int termOrder, List<PrerequisiteGroup> groups,
			List<PlannedCourse> planned, Map<Integer, List<String>> placements, UpperDivisionGeRule udRule) {
		List<String> missing = new PrerequisiteCheck().findMissing(courseId, termOrder, groups, planned, placements);
		missing.addAll(udRule.findMissing(courseId, termOrder, planned, 0));
		return missing;
	}

	private static int findTerm(RoadmapCourse course, int earliest, List<SessionInfo> sessions,
			List<PlannedCourse> planned, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			UpperDivisionGeRule udRule) {
		// JDBC reads NULL roadmap position as 0.
		int position = course.getSuggestedTerm();
		if (position <= 0) {
			return sessions.size();
		}
		return findTerm(course.getCourseId(), Double.parseDouble(course.getUnits()), Math.max(0, position - earliest),
				sessions, planned, groups, placements, udRule);
	}

	private static int findTerm(int courseId, double units, int firstIndex, List<SessionInfo> sessions,
			List<PlannedCourse> planned, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			UpperDivisionGeRule udRule) {
		for (int index = firstIndex; index < sessions.size(); index++) {
			SessionInfo term = sessions.get(index);
			if (hasRoom(units, term, planned)
					&& missingPrerequisites(courseId, term.getTermOrder(), groups, planned, placements,
							udRule).isEmpty()) {
				return index;
			}
		}
		return sessions.size();
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

	private static class GePickOrder implements Comparator<TranscriptCourse> {
		private final CourseCodeOrder codeOrder = new CourseCodeOrder();
		private final Map<Integer, String> risks;

		GePickOrder(Map<Integer, String> risks) {
			this.risks = risks;
		}

		@Override
		public int compare(TranscriptCourse first, TranscriptCourse second) {
			int risk = Integer.compare(riskOrder(risks.get(first.getCourseId())),
					riskOrder(risks.get(second.getCourseId())));
			if (risk != 0) {
				return risk;
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
