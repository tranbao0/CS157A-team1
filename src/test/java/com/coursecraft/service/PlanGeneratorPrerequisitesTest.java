package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.coursecraft.dao.BottleneckScoreDao;
import com.coursecraft.dao.CourseDao;
import com.coursecraft.dao.GeRuleDao;
import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.PrerequisiteDao;
import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.RoadmapGeStatus;
import com.coursecraft.model.TranscriptCourse;
import com.coursecraft.model.UnplacedCourse;
import com.coursecraft.util.Db;

class PlanGeneratorPrerequisitesTest {

	@Test
	void generatedPlanCoversCoreGeAndMeetsPrerequisites() throws SQLException {
		List<String> failures = new ArrayList<>();
		try (Connection connection = Db.getConnection()) {
			List<Integer> students = new ArrayList<>();
			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT student_id FROM students ORDER BY student_id");
					ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					students.add(rows.getInt("student_id"));
				}
			}
			for (int studentId : students) {
				checkProposals(connection, studentId, failures);
			}
		}
		assertEquals(List.of(), failures, String.join("\n", failures));
	}

	private void checkProposals(Connection connection, int studentId, List<String> failures) throws SQLException {
		GeneratedPlan plan = new PlanGenerator().preview(connection, studentId);
		List<PlannedCourse> planned = new PlanEntryDao().findForStudent(connection, studentId);
		int firstProposal = planned.size();
		Set<Integer> suggestedIds = new HashSet<>();
		for (ProposedTerm term : plan.getTerms()) {
			if (term.getUnits() > UnitRules.NORMAL_LIMIT) {
				failures.add("Student " + studentId + ", " + term.getLabel() + ": " + term.getUnits() + " units");
			}
			for (ProposedEntry entry : term.getEntries()) {
				if (entry.isSuggested()) {
					suggestedIds.add(entry.getCourseId());
				}
				planned.add(new PlannedCourse(0, entry.getCourseId(), entry.getCode(), entry.getTitle(),
						entry.getUnits(), term.getTermId(), term.getLabel(), null, term.getTermOrder(), null));
			}
		}

		int year = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
		PrerequisiteDao prerequisites = new PrerequisiteDao();
		List<PrerequisiteGroup> groups = prerequisites.findOpenGroups(connection, studentId, year);
		Map<Integer, List<String>> placements = prerequisites.findPlacementOptions(connection);
		checkCoreGe(connection, studentId, plan, planned, suggestedIds, groups, placements, failures);

		PrerequisiteCheck check = new PrerequisiteCheck();
		UpperDivisionGeRule udRule = new GeRuleDao().load(connection, studentId);
		for (int i = firstProposal; i < planned.size(); i++) {
			PlannedCourse course = planned.get(i);
			List<String> missing = check.findMissing(course.getCourseId(), course.getTermOrder(), groups, planned,
					placements);
			missing.addAll(udRule.findMissing(course.getCourseId(), course.getTermOrder(), planned, 0));
			if (!missing.isEmpty()) {
				failures.add("Student " + studentId + ", " + course.getCode() + ", " + course.getTermLabel()
						+ ": " + String.join("; ", missing));
			}
		}
	}

	private void checkCoreGe(Connection connection, int studentId, GeneratedPlan plan, List<PlannedCourse> planned,
			Set<Integer> suggestedIds, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			List<String> failures) throws SQLException {
		Set<Integer> plannedIds = new HashSet<>();
		for (PlannedCourse row : planned) {
			plannedIds.add(row.getCourseId());
		}
		Set<Integer> unplacedIds = new HashSet<>();
		for (UnplacedCourse row : plan.getUnplaced()) {
			if (!unplacedIds.add(row.getCourseId()) || plannedIds.contains(row.getCourseId())) {
				failures.add("Student " + studentId + ", " + row.getCode() + ": conflicting plan rows");
			}
		}
		List<TranscriptCourse> catalog = new CourseDao().findForTranscript(connection, studentId);
		Set<Integer> requiredIds = new HashSet<>();
		RoadmapDao roadmap = new RoadmapDao();
		for (RoadmapCourse course : roadmap.findCoursesForStudent(connection, studentId)) {
			if (!"Elective".equals(course.getRequirementType())) {
				requiredIds.add(course.getCourseId());
			}
		}
		Set<String> reserved = new HashSet<>();
		for (TranscriptCourse course : catalog) {
			if (requiredIds.contains(course.getCourseId()) && unplacedIds.contains(course.getCourseId())) {
				reserved.addAll(coveredAreas(course));
			}
		}
		Map<Integer, String> risks = new BottleneckScoreDao().findNewestRisks(connection);
		Set<String> covered = new HashSet<>();
		for (TranscriptCourse course : catalog) {
			if (!course.isPassed() && !plannedIds.contains(course.getCourseId())) {
				continue;
			}
			Set<String> areas = coveredAreas(course);
			covered.addAll(areas);
			if (suggestedIds.contains(course.getCourseId())) {
				for (String area : areas) {
					if (reserved.contains(area)) {
						failures.add("Student " + studentId + ", area " + area + ": "
								+ course.getCode() + " overlaps an unplaced roadmap course");
					}
				}
				checkPlacementOnlyPick(studentId, course, catalog, requiredIds, groups, placements, failures);
				for (TranscriptCourse other : catalog) {
					if (other.getCourseId() == course.getCourseId()
							|| (!other.isPassed() && !plannedIds.contains(other.getCourseId()))) {
						continue;
					}
					for (String area : coveredAreas(other)) {
						if (areas.contains(area)) {
							failures.add("Student " + studentId + ", area " + area + ": "
									+ course.getCode() + " overlaps " + other.getCode());
						}
					}
				}
			}
			Pattern code = Pattern.compile("\\b" + Pattern.quote(course.getCode()) + "\\b");
			for (UnplacedCourse row : plan.getUnplaced()) {
				if (code.matcher(row.getReason()).find()) {
					failures.add("Student " + studentId + ", " + row.getCode()
							+ ": reason names " + course.getCode() + ": " + row.getReason());
				}
			}
		}
		CourseCodeOrder order = new CourseCodeOrder();
		for (RoadmapGeStatus area : roadmap.findGeAreasForStudent(connection, studentId)) {
			String code = area.getCode();
			if (code.startsWith("UD") || area.isFulfilled() || covered.contains(code) || reserved.contains(code)) {
				continue;
			}
			TranscriptCourse top = null;
			for (TranscriptCourse course : catalog) {
				if (!code.equals(course.getGeAreaCode()) || course.isPassed()) {
					continue;
				}
				int risk = riskRank(risks.get(course.getCourseId()));
				if (top == null || risk < riskRank(risks.get(top.getCourseId()))
						|| (risk == riskRank(risks.get(top.getCourseId()))
								&& order.compare(course.getCode(), top.getCode()) < 0)) {
					top = course;
				}
			}
			if (top != null && !unplacedIds.contains(top.getCourseId())) {
				failures.add("Student " + studentId + ", area " + code + ": missing " + top.getCode());
			}
		}
	}

	private void checkPlacementOnlyPick(int studentId, TranscriptCourse pick, List<TranscriptCourse> catalog,
			Set<Integer> requiredIds, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			List<String> failures) {
		String area = pick.getGeAreaCode();
		if (area == null) {
			return;
		}
		for (PrerequisiteGroup group : groups) {
			if (!group.hasOption(pick.getCourseId()) || !requiredIds.contains(group.getCourseId())
					|| placements.getOrDefault(group.getGroupId(), List.of()).isEmpty()) {
				continue;
			}
			for (TranscriptCourse course : catalog) {
				if (course.getCourseId() == group.getCourseId() && coveredAreas(course).contains(area)) {
					failures.add("Student " + studentId + ": " + pick.getCode()
							+ " meets a placement-accepting group for " + course.getCode()
							+ ", which already covers area " + area);
				}
			}
		}
	}

	private Set<String> coveredAreas(TranscriptCourse course) {
		String area = course.getGeAreaCode();
		if (area == null) {
			return Set.of();
		}
		if (Set.of("5A", "5B").contains(area) && Double.parseDouble(course.getUnits()) >= 4) {
			return Set.of(area, "5C");
		}
		return Set.of(area);
	}

	private int riskRank(String risk) {
		if ("High".equals(risk)) {
			return 2;
		}
		if ("Medium".equals(risk)) {
			return 1;
		}
		return 0;
	}
}
