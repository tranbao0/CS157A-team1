package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.SessionInfo;
import com.coursecraft.model.UnplacedCourse;

class PlanGeneratorTest {

	@Test
	void mapsMissingRequiredCoursesFromEarliestPosition() {
		List<RoadmapCourse> courses = List.of(
				course(1, 1, "Major", true, null),
				course(2, 1, "Support", false, "Fall 2026"),
				course(3, 1, "Elective", false, null),
				course(4, 2, "Major", false, null),
				course(5, 2, "Support", false, null),
				course(6, 3, "Major", false, null));
		GeneratedPlan plan = PlanGenerator.mapCourses(courses, sessions(), List.of(planned(2, 20264), planned(7, 20264)), List.of(), Map.of());
		assertEquals(2, plan.getTerms().size());
		assertEquals(7, plan.getTerms().get(0).getTermId());
		assertEquals(12, plan.getTerms().get(0).getUnits());
		assertEquals("full-time", plan.getTerms().get(0).getUnitStatus());
		assertEquals(19, plan.getTerms().get(0).getUnitCap());
		assertEquals(4, plan.getTerms().get(0).getEntries().get(0).getCourseId());
		assertEquals(9, plan.getTerms().get(0).getEntries().get(0).getSessionId());
		assertFalse(plan.getTerms().get(0).getEntries().get(0).isSuggested());
		assertEquals(9, plan.getTerms().get(1).getTermId());
		assertEquals(0, plan.getUnplaced().size());
	}

	@Test
	void keepsSpaceForPositionWithNoMissingCourse() {
		GeneratedPlan plan = PlanGenerator.mapCourses(List.of(
				course(1, 2, "Major", false, null),
				course(2, 3, "Major", true, null),
				course(3, 4, "Major", false, null)), sessions(), List.of(), List.of(), Map.of());
		assertEquals(2, plan.getTerms().size());
		assertEquals(7, plan.getTerms().get(0).getTermId());
		assertEquals(11, plan.getTerms().get(1).getTermId());
	}

	@Test
	void listsCourseThatMapsPastLastTermAsUnplaced() {
		GeneratedPlan plan = PlanGenerator.mapCourses(List.of(
				course(1, 1, "Major", false, null),
				course(2, 4, "Major", false, null)), sessions(), List.of(), List.of(), Map.of());
		assertEquals(1, plan.getTerms().size());
		assertEquals(1, plan.getUnplaced().size());
		assertEquals(2, plan.getUnplaced().get(0).getCourseId());
		assertEquals("CS 2 has no term available yet.", plan.getUnplaced().get(0).getReason());
	}

	@Test
	void leavesMissingPositionUnplacedAndMapsOtherTypesInOrder() {
		GeneratedPlan plan = PlanGenerator.mapCourses(List.of(
				course(1, 0, "Major", false, null),
				course(3, 4, "New requirement", false, null),
				course(4, 3, "Major", false, null),
				course(2, 2, null, false, null),
				course(5, 0, "Elective", false, null)), sessions(), List.of(), List.of(), Map.of());
		assertEquals(3, plan.getTerms().size());
		assertEquals(7, plan.getTerms().get(0).getTermId());
		assertEquals(2, plan.getTerms().get(0).getEntries().get(0).getCourseId());
		assertEquals(9, plan.getTerms().get(1).getTermId());
		assertEquals(11, plan.getTerms().get(2).getTermId());
		assertEquals(3, plan.getTerms().get(2).getEntries().get(0).getCourseId());
		assertEquals(1, plan.getUnplaced().size());
		assertEquals(1, plan.getUnplaced().get(0).getCourseId());
		assertEquals("Roadmap gives CS 1 no term.", plan.getUnplaced().get(0).getReason());
	}

	@Test
	void movesDependentAfterPrerequisiteFromLaterRoadmapPosition() {
		assertEquals(List.of("9:2", "11:1"), schedule(List.of(required(1, 1), required(2, 2)),
				List.of(group(1, 2)), List.of(), Map.of()));
	}

	@Test
	void movesEachCourseInPrerequisiteChain() {
		assertEquals(List.of("7:3", "9:2", "11:1"), schedule(List.of(required(1, 1), required(2, 1), required(3, 1)),
				List.of(group(1, 2), group(2, 3)), List.of(), Map.of()));
	}

	@Test
	void placesCourseWhenOneOptionMeetsGroup() {
		assertEquals(List.of("7:2", "9:1", "11:3"), schedule(List.of(required(1, 1), required(2, 1), required(3, 1)),
				List.of(group(1, 2, 3), group(3, 1)), List.of(), Map.of()));
	}

	@Test
	void movesDependentAfterLaterSavedPrerequisite() {
		assertEquals(List.of("11:1"), schedule(List.of(required(1, 1)), List.of(group(1, 2)),
				List.of(planned(2, 20272)), Map.of()));
	}

	@Test
	void listsMissingCourseAndPlacementChoices() {
		assertEquals(List.of("1:Needs CS 4 or placement M-I first.", "2:Needs placement M-II first."),
				schedule(List.of(required(1, 1), required(2, 1)), List.of(group(1, 4), group(2)),
						List.of(), Map.of(1, List.of("M-I"), 2, List.of("M-II"))));
	}

	@Test
	void listsCourseWhenMoveNeedsAnotherTerm() {
		assertEquals(List.of("1:CS 1 has no term available yet."), schedule(List.of(required(1, 1)),
				List.of(group(1, 2)), List.of(planned(2, 20274)), Map.of()));
	}

	@Test
	void listsDependentOfUnplacedCourseWithItsOwnReason() {
		assertEquals(List.of("2:Needs CS 3 first.", "1:Needs CS 2 first."), schedule(List.of(required(1, 1),
				required(2, 1)), List.of(group(1, 2), group(2, 3)), List.of(), Map.of()));
	}

	@Test
	void listsCoursesInCycle() {
		assertEquals(List.of("1:Needs CS 2 first.", "2:Needs CS 1 first."), schedule(List.of(required(1, 1),
				required(2, 1)), List.of(group(1, 2), group(2, 1)), List.of(), Map.of()));
	}

	@Test
	void usesSavedOrOptionBeforePendingOption() {
		assertEquals(List.of("7:1", "9:3"), schedule(List.of(required(1, 1), required(3, 2)),
				List.of(group(1, 2, 3)), List.of(planned(2, 20262)), Map.of()));
	}

	@Test
	void sortsProposedRowsByCourseCodeWithinTerm() {
		assertEquals(List.of("7:2", "7:10", "9:3"), schedule(List.of(required(2, 1), required(10, 1),
				required(3, 2)), List.of(group(2, 3, 4)), List.of(planned(4, 20262)), Map.of()));
	}

	private List<String> schedule(List<RoadmapCourse> courses, List<PrerequisiteGroup> groups,
			List<PlannedCourse> saved, Map<Integer, List<String>> placements) {
		GeneratedPlan plan = PlanGenerator.mapCourses(courses, sessions(), saved, groups, placements);
		List<String> results = new ArrayList<>();
		List<PlannedCourse> proposed = new ArrayList<>();
		for (ProposedTerm term : plan.getTerms()) {
			for (ProposedEntry entry : term.getEntries()) {
				results.add(term.getTermId() + ":" + entry.getCourseId());
				proposed.add(planned(entry.getCourseId(), term.getTermOrder()));
			}
		}
		List<PlannedCourse> all = new ArrayList<>(saved);
		all.addAll(proposed);
		for (PlannedCourse entry : proposed) {
			assertEquals(List.of(), new PrerequisiteCheck().findMissing(entry.getCourseId(), entry.getTermOrder(),
					groups, all, placements));
		}
		for (UnplacedCourse course : plan.getUnplaced()) {
			results.add(course.getCourseId() + ":" + course.getReason());
		}
		return results;
	}

	private RoadmapCourse required(int id, int position) {
		return course(id, position, "Major", false, null);
	}

	private PlannedCourse planned(int id, int order) {
		return new PlannedCourse(0, id, "CS " + id, "", 3, 7, "", "", order, null);
	}

	private PrerequisiteGroup group(int id, int... options) {
		PrerequisiteGroup group = new PrerequisiteGroup(id, id);
		for (int option : options) {
			group.addOption(option, "CS " + option);
		}
		return group;
	}

	private RoadmapCourse course(int id, int position, String type, boolean passed, String planned) {
		return new RoadmapCourse(id, "CS", String.valueOf(id), "Course " + id, "3",
				position, type, passed, planned);
	}

	private List<SessionInfo> sessions() {
		return List.of(
				new SessionInfo(9, 7, "Fall", "Fall 2026", "Fall 2026", 20264),
				new SessionInfo(11, 9, "Spring", "Spring 2027", "Spring 2027", 20272),
				new SessionInfo(12, 11, "Fall", "Fall 2027", "Fall 2027", 20274));
	}
}
