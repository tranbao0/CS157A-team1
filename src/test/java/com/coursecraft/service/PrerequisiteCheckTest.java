package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;

class PrerequisiteCheckTest {
    private final PrerequisiteCheck check = new PrerequisiteCheck();

    private PlannedCourse planned(int courseId, int termOrder) {
        return new PlannedCourse(1, courseId, "CS " + courseId, "Course", 3, 1,
                "Fall 2026", "Fall", termOrder, null);
    }

    @Test
    void onlyEarlierTermsMeetAnAlternativeAndEveryGroupIsRequired() {
        PrerequisiteGroup first = new PrerequisiteGroup(1, 100);
        first.addOption(10, "CS 10");
        first.addOption(11, "CS 11");
        PrerequisiteGroup second = new PrerequisiteGroup(2, 100);
        second.addOption(20, "CS 20");
        List<PrerequisiteGroup> groups = List.of(first, second);
        assertEquals(List.of("CS 20"), check.findMissing(100, 20272, groups,
                List.of(planned(11, 20264), planned(20, 20272)), Map.of()));
        assertEquals(List.of("CS 20"), check.findMissing(100, 20272, groups,
                List.of(planned(10, 20264), planned(20, 20274)), Map.of()));
        assertEquals(List.of(), check.findMissing(100, 20272, groups,
                List.of(planned(11, 20264), planned(20, 20264)), Map.of()));
    }

    @Test
    void placementOnlyAndUnlistedGroupsKeepTheCourseUnavailable() {
        PrerequisiteGroup placement = new PrerequisiteGroup(1, 100);
        PrerequisiteGroup unlisted = new PrerequisiteGroup(2, 100);
        assertEquals(List.of("placement M-II (PPA 70+)", "a prerequisite with no listed options"),
                check.findMissing(100, 20264, List.of(placement, unlisted), List.of(),
                        Map.of(1, List.of("M-II (PPA 70+)"))));
    }

    @Test
    void groupsForOtherCoursesDoNotBlockCoursesWithNoPrerequisites() {
        PrerequisiteGroup other = new PrerequisiteGroup(1, 200);
        other.addOption(10, "CS 10");
        assertEquals(List.of(), check.findMissing(100, 20264, List.of(other), List.of(), Map.of()));
    }
}
