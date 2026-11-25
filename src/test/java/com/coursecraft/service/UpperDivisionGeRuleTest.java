package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.coursecraft.model.PlannedCourse;

// no database, rule gets made-up courses: 100 is UD GE course, 1-5 are core GE courses, 9 is 57-unit filler
class UpperDivisionGeRuleTest {

	private static final int UD_COURSE = 100;

	private final Map<Integer, String> areas = new HashMap<>();
	private final Map<Integer, Double> units = new HashMap<>();

	UpperDivisionGeRuleTest() {
		course(UD_COURSE, "UD3", 3);
		course(1, "1A", 3);
		course(2, "2", 3);
		course(3, "5A", 4);
		course(4, "5A", 3);
		course(5, "5C", 1);
		course(9, null, 57);
	}

	private void course(int id, String area, double courseUnits) {
		if (area != null) {
			areas.put(id, area);
		}
		units.put(id, courseUnits);
	}

	private UpperDivisionGeRule rule(Set<Integer> passed, Set<String> placements, Set<String> core) {
		return new UpperDivisionGeRule(areas, units, passed, placements, core);
	}

	private PlannedCourse planned(int courseId, int termOrder) {
		return new PlannedCourse(1, courseId, "C " + courseId, "Course", units.get(courseId), 1,
				"Fall 2026", "Fall", termOrder, null);
	}

	@Test
	void courseWithoutUdAreaIsNotChecked() {
		UpperDivisionGeRule rule = rule(Set.of(), Set.of(), Set.of("1A"));
		assertFalse(rule.appliesTo(1));
		assertEquals(List.of(), rule.findMissing(1, 20272, List.of(), 0));
	}

	@Test
	void listsMissingStandingAndCoreAreas() {
		UpperDivisionGeRule rule = rule(Set.of(1), Set.of(), Set.of("1A", "2", "5A"));
		assertEquals(List.of("upper division standing (60 units passed or planned before this term, 3 so far)",
				"core GE Areas 2, 5A"), rule.findMissing(UD_COURSE, 20272, List.of(), 0));
	}

	@Test
	void passedAndEarlierPlannedCoursesAndPlacementsCount() {
		UpperDivisionGeRule rule = rule(Set.of(9), Set.of("2"), Set.of("1A", "2", "5A", "5C"));
		// 57 passed + 3 + 4 planned earlier = 64 units, M-I placement meets 2, 4-unit 5A course also meets 5C
		List<PlannedCourse> plan = List.of(planned(1, 20264), planned(3, 20264), planned(UD_COURSE, 20272));
		assertEquals(List.of(), rule.findMissing(UD_COURSE, 20272, plan, 0));
	}

	@Test
	void sameTermDoesNotCountAndShortLabCourseLeavesFiveC() {
		UpperDivisionGeRule rule = rule(Set.of(9), Set.of(), Set.of("1A", "5A", "5C"));
		List<PlannedCourse> plan = List.of(planned(1, 20264), planned(4, 20272));
		assertEquals(List.of("core GE Areas 5A, 5C"), rule.findMissing(UD_COURSE, 20272, plan, 0));
	}

	@Test
	void removingCourseThatMeetsRuleBreaksLaterUdCourse() {
		UpperDivisionGeRule rule = rule(Set.of(9), Set.of(), Set.of("1A"));
		List<PlannedCourse> plan = List.of(planned(1, 20264), planned(UD_COURSE, 20272));
		assertTrue(rule.isBrokenByRemoving(UD_COURSE, 20272, plan, 1));
		assertFalse(rule.isBrokenByRemoving(UD_COURSE, 20272, plan, UD_COURSE));
	}
}
