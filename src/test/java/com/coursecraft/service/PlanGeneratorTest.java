package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.SessionInfo;

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
		GeneratedPlan plan = PlanGenerator.mapCourses(courses, sessions(), Map.of(7, 6.0));
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
				course(3, 4, "Major", false, null)), sessions(), Map.of());
		assertEquals(2, plan.getTerms().size());
		assertEquals(7, plan.getTerms().get(0).getTermId());
		assertEquals(11, plan.getTerms().get(1).getTermId());
	}

	@Test
	void listsCourseThatMapsPastLastTermAsUnplaced() {
		GeneratedPlan plan = PlanGenerator.mapCourses(List.of(
				course(1, 1, "Major", false, null),
				course(2, 4, "Major", false, null)), sessions(), Map.of());
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
				course(5, 0, "Elective", false, null)), sessions(), Map.of());
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
