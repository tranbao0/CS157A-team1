package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.SessionInfo;
import com.coursecraft.model.TranscriptCourse;
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
		GeneratedPlan plan = checkedPlan(courses, sessions(), List.of(planned(2, 20264), planned(7, 20264)), List.of(), Map.of(), Map.of(), Map.of());
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
		GeneratedPlan plan = checkedPlan(List.of(
				course(1, 2, "Major", false, null),
				course(2, 3, "Major", true, null),
				course(3, 4, "Major", false, null)), sessions(), List.of(), List.of(), Map.of(), Map.of(), Map.of());
		assertEquals(2, plan.getTerms().size());
		assertEquals(7, plan.getTerms().get(0).getTermId());
		assertEquals(11, plan.getTerms().get(1).getTermId());
	}

	@Test
	void listsCourseThatMapsPastLastTermAsUnplaced() {
		GeneratedPlan plan = checkedPlan(List.of(
				course(1, 1, "Major", false, null),
				course(2, 4, "Major", false, null)), sessions(), List.of(), List.of(), Map.of(), Map.of(), Map.of());
		assertEquals(1, plan.getTerms().size());
		assertEquals(1, plan.getUnplaced().size());
		assertEquals(2, plan.getUnplaced().get(0).getCourseId());
		assertEquals("CS 2 has no term available yet.", plan.getUnplaced().get(0).getReason());
	}

	@Test
	void leavesMissingPositionUnplacedAndMapsOtherTypesInOrder() {
		GeneratedPlan plan = checkedPlan(List.of(
				course(1, 0, "Major", false, null),
				course(3, 4, "New requirement", false, null),
				course(4, 3, "Major", false, null),
				course(2, 2, null, false, null),
				course(5, 0, "Elective", false, null)), sessions(), List.of(), List.of(), Map.of(), Map.of(), Map.of());
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
	void listsPrerequisiteThatFallsInLastTerm() {
		assertEquals(List.of("1:Needs CS 2 first."), schedule(List.of(required(1, 1)),
				List.of(group(1, 2)), List.of(planned(2, 20274)), Map.of()));
	}

	@Test
	void listsDependentOfUnplacedCourseWithItsOwnReason() {
		assertEquals(List.of("1:Needs CS 2 first.", "2:Needs CS 3 first."), schedule(List.of(required(1, 1),
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

	@Test
	void suggestsOneCourseBeforeDependent() {
		assertEquals(List.of("7:4", "9:2*", "11:1"), suggestions(List.of(required(1, 1), required(4, 1)),
				List.of(group(1, 2, 3), group(2, 4), group(3, 4)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void leavesPlacementGroupUnplaced() {
		assertEquals(List.of("1:Needs CS 2 or placement M-I first."), suggestions(List.of(required(1, 1)),
				List.of(group(1, 2)), List.of(), Map.of(1, List.of("M-I")), Map.of()));
	}

	@Test
	void sharesSuggestionWhenRoadmapAlreadyMeetsPlacementGroup() {
		assertEquals(List.of("7:2", "7:10*", "9:1", "9:4"), suggestions(
				List.of(required(1, 2), required(2, 1), required(4, 1)),
				List.of(group(1, 2, 10), group(4, 10)), List.of(), Map.of(1, List.of("M-I")), Map.of()));
	}

	@Test
	void sharesSuggestionWhenSavedCourseAlreadyMeetsPlacementGroup() {
		assertEquals(List.of("7:10*", "9:1", "9:4"), suggestions(List.of(required(1, 2), required(4, 1)),
				List.of(group(1, 3, 10), group(4, 10)), List.of(planned(3, 20264)), Map.of(1, List.of("M-I")), Map.of()));
	}

	@Test
	void countsSuggestionFromCourseOnlyGroupTowardPlacementGroup() {
		assertEquals(List.of("7:10*", "9:1", "9:4"), suggestions(List.of(required(1, 2), required(4, 1)),
				List.of(group(1, 10), group(4, 10)), List.of(), Map.of(1, List.of("M-I")), Map.of()));
	}

	@Test
	void triesSuggestionWhenRoadmapOptionIsInLastTerm() {
		assertEquals(List.of("7:10*", "9:1", "11:4"), suggestions(List.of(required(1, 1), required(4, 3)),
				List.of(group(1, 4, 10)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void suggestsCoursesForBothGroups() {
		PrerequisiteGroup second = new PrerequisiteGroup(5, 1);
		second.addOption(3, "CS 3");
		assertEquals(List.of("7:2*", "7:3*", "9:1"), suggestions(List.of(required(1, 1)),
				List.of(group(1, 2), second), List.of(), Map.of(), Map.of()));
	}

	@Test
	void sharesOneSuggestionBetweenDependents() {
		assertEquals(List.of("7:3*", "9:1", "9:2"), suggestions(List.of(required(1, 1), required(2, 1)),
				List.of(group(1, 3), group(2, 3)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void skipsGroupsAlreadyMetByPassedSavedOrProposedCourses() {
		assertEquals(List.of("7:3", "7:10*", "9:1"), suggestions(
				List.of(required(1, 1), required(3, 1), course(8, 1, "Major", true, null)),
				List.of(group(1, 2), extraGroup(6, 3), extraGroup(7, 10)),
				List.of(planned(2, 20262)), Map.of(), Map.of()));
	}

	@Test
	void waitsForLaterRoadmapOptionThatNeedsSuggestion() {
		assertEquals(List.of("7:3*", "9:2", "11:1"), suggestions(List.of(required(1, 1), required(2, 2)),
				List.of(group(1, 2, 10), group(2, 3)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void waitsForSamePositionRoadmapOptionThatNeedsSuggestion() {
		assertEquals(List.of("7:3*", "9:2", "11:1"), suggestions(List.of(required(1, 1), required(2, 1)),
				List.of(group(1, 2, 10), group(2, 3)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void suggestsAnOptionToBreakRoadmapCycle() {
		assertEquals(List.of("7:10*", "9:1", "11:2"), suggestions(List.of(required(1, 1), required(2, 1)),
				List.of(group(1, 2, 10), group(2, 1)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void leavesCourseUnplacedWhenAnotherGroupCannotBeMet() {
		assertEquals(List.of("1:Needs CS 2 and CS 3 first."), suggestions(List.of(required(1, 1)),
				List.of(group(1, 2), extraGroup(5, 3), group(3, 4)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void keepsSavedOptionWhenSuggestingForAnotherGroup() {
		assertEquals(List.of("7:3*", "9:1"), suggestions(List.of(required(1, 1)),
				List.of(group(1, 2), extraGroup(5, 3)), List.of(planned(2, 20262)), Map.of(), Map.of()));
	}

	@Test
	void picksOptionsThatKeepTheWholeRoadmapWithinThreeTerms() {
		assertEquals(List.of("7:10*", "7:12*", "9:2", "9:3", "11:1"), suggestions(
				List.of(required(1, 1), required(2, 1), required(3, 2)),
				List.of(group(1, 2), group(2, 3, 10), group(3, 12)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void picksOneOptionThatBreaksTheWholeCycle() {
		assertEquals(List.of("7:11*", "9:2", "11:1", "11:3"), schedule(
				List.of(required(1, 1), required(2, 1), required(3, 1)),
				List.of(group(1, 2, 10), group(2, 3, 11), group(3, 2, 12)), List.of(), Map.of(), catalog(), Map.of(), sessions(4)));
	}

	@Test
	void picksOneOptionThatCoversBothGroups() {
		assertEquals(List.of("7:10*", "9:1", "9:4"), suggestions(List.of(required(1, 1), required(4, 1)),
				List.of(group(1, 2, 10), group(4, 3, 10)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void rejectsTrialWithUnplacedSuggestionEvenWhenItPlacesMoreRoadmapCourses() {
		List<RoadmapCourse> courses = List.of(required(1, 1));
		List<PrerequisiteGroup> groups = List.of(group(1, 2), group(3, 4));
		List<RoadmapCourse> chosen = List.of(required(2, 1), required(3, 1));
		PlanGenerator.Placement trial = new PlanGenerator.Placement(courses, 1, sessions(), List.of(),
				groups, Map.of(), chosen);
		assertEquals(0, trial.remaining());
		assertFalse(PlanGenerator.isLegal(trial, chosen, 0, groups, Map.of()));
	}

	@Test
	void breaksEqualCountAndSizeTieByLowerCourseNumbers() {
		assertEquals(List.of("7:2*", "7:3*", "9:1", "9:4", "9:5", "9:7", "6:Needs CS 10 first."),
				suggestions(List.of(required(1, 1), required(4, 1), required(5, 1), required(6, 1), required(7, 1)),
						List.of(group(1, 2, 3), group(4, 2, 10), group(5, 3), group(6, 10), group(7, 2)),
						List.of(), Map.of(7, List.of("M-I")), Map.of()));
	}

	@Test
	void leavesOutOptionCoveredByOtherSuggestions() {
		assertEquals(List.of("7:3*", "7:10*", "9:1", "9:4", "9:5", "9:6"), suggestions(
				List.of(required(1, 1), required(4, 1), required(5, 1), required(6, 1)),
				List.of(group(1, 2, 3), group(4, 2, 10), group(5, 3), group(6, 10)),
				List.of(), Map.of(), Map.of()));
	}

	@Test
	void prefersLowerRiskSetWhenItPlacesTheSameCount() {
		PrerequisiteGroup second = extraGroup(5, 3);
		second.addOption(11, "CS 11");
		assertEquals(List.of("7:2*", "7:10*", "9:4", "11:11*", "12:1"), schedule(
				List.of(required(1, 1), required(4, 1)),
				List.of(group(1, 2), second, group(4, 10), group(11, 4)),
				List.of(), Map.of(), catalog(), Map.of(List.of(7, 3), "High"), sessions(4)));
	}

	@Test
	void placesEachSuggestionAfterItsPrerequisites() {
		assertEquals(List.of("7:10*", "9:1", "11:3", "11:11*", "12:2"), schedule(
				List.of(required(1, 1), required(2, 1), required(3, 3)),
				List.of(group(1, 2, 10), group(2, 11), group(11, 1, 3)),
				List.of(), Map.of(), catalog(), Map.of(), sessions(5)));
	}

	@Test
	void choosesOptionsThatFitBeforeTheirDependent() {
		assertEquals(List.of("7:11*", "7:12*", "9:1", "9:5"), suggestions(
				List.of(required(1, 1), required(5, 2)),
				List.of(group(1, 11), extraGroup(7, 10, 12), group(10, 5)),
				List.of(), Map.of(), Map.of()));
	}

	@Test
	void placesSuggestionAndItsSuggestedPrerequisite() {
		assertEquals(List.of("7:11*", "9:10*", "11:1"), suggestions(List.of(required(1, 1)),
				List.of(group(1, 10), group(10, 11)), List.of(), Map.of(), Map.of()));
	}

	private PrerequisiteGroup extraGroup(int id, int... options) {
		PrerequisiteGroup group = new PrerequisiteGroup(id, 1);
		for (int option : options) {
			group.addOption(option, "CS " + option);
		}
		return group;
	}

	@Test
	void keepsSuggestionThatAnotherSuggestionNeeds() {
		assertEquals(List.of("7:3*", "9:2*", "11:1"), suggestions(List.of(required(1, 1)),
				List.of(group(1, 2), group(2, 3)), List.of(), Map.of(), Map.of()));
	}

	@Test
	void skipsOptionWhenNoLaterTermRemainsForDependent() {
		assertEquals(List.of("1:Needs CS 2 first."), suggestions(List.of(required(1, 1)),
				List.of(group(1, 2), group(2, 3)), List.of(planned(3, 20272)), Map.of(), Map.of()));
	}

	@Test
	void picksLowestRiskThenCourseCodeInCandidateTerm() {
		assertEquals(List.of("7:3*", "9:1"), suggestions(List.of(required(1, 1)), List.of(group(1, 10, 3, 2)),
				List.of(), Map.of(), Map.of(List.of(7, 2), "Medium", List.of(7, 10), "Low", List.of(9, 3), "High")));
	}

	@Test
	void keepsCourseOnlySuggestionWhenAnotherSuggestionMeetsPlacementGroup() {
		assertEquals(List.of("7:10*", "7:11*", "9:1", "9:4"), suggestions(List.of(required(1, 2), required(4, 1)),
				fixtureGroups(new int[][] {{1, 11}, {1, 10}, {4, 10}}), List.of(), Map.of(2, List.of("M-I")), Map.of()));
	}

	@Test
	void movesAlreadyPlacedPrerequisiteEarlierSoItsDependentFits() {
		assertEquals(List.of("7:10*", "9:2", "11:1"), suggestions(
				List.of(required(1, 1), required(2, 1)), List.of(group(1, 2), group(2, 3, 10)),
				List.of(planned(3, 20272)), Map.of(), Map.of()));
	}

	@Test
	void searchesCombinedChoicesForInterdependentRoadmapCourses() {
		assertEquals(List.of("7:100*", "7:103*", "7:104*", "8:2", "8:4", "8:5", "8:6", "9:1", "9:3"), fixture(
				new int[] {1, 2, 2, 2, 2, 2}, new int[][] {{1, 2}, {1, 4}, {2, 5, 104}, {3, 2, 5}, {4, 102, 1, 103},
						{6, 100, 1, 101}, {6, 103, 3, 102}, {102, 101}}, Map.of(8, List.of("P8")), 3,
				Map.of(List.of(7, 100), "Medium", List.of(7, 103), "High")));
	}

	@Test
	void placesCourseAfterSuggestedChainAndPlacementOption() {
		assertEquals(List.of("7:1", "7:102*", "8:104*", "10:3", "10:4", "11:2"), fixture(new int[] {1, 3, 4, 4},
				new int[][] {{2, 3}, {2, 104, 103}, {3, 102}, {3, 104}, {4, 102}, {100, 1, 104, 101}, {104, 1}},
				Map.of(2, List.of("P2"), 3, List.of("P3")), 5, Map.of()));
	}

	@Test
	void leavesCourseUnplacedWhenAnOutsideCourseWouldOnlyMeetPlacement() {
		assertEquals(List.of("7:102*", "7:103*", "8:1", "8:3", "9:2", "9:5", "4:Needs CS 100 or placement P5 first."),
				fixture(new int[] {2, 3, 2, 1, 1}, new int[][] {{1, 102}, {2, 103}, {2, 5, 104, 102}, {4, 5, 102},
						{4, 100}, {5, 102, 103}, {5, 100, 1}, {100, 2, 3}, {101, 102}, {104, 100, 5, 103}},
						Map.of(1, List.of("P1"), 4, List.of("P4"), 5, List.of("P5")), 4, Map.of()));
	}

	@Test
	void leavesPlacementAndLastTermOnlySuggestionsOut() {
		assertEquals(List.of("8:2", "1:Needs CS 3 or CS 102 or placement P1 first.", "3:Needs CS 101, and CS 102 or CS 1 first.",
				"4:Needs CS 3 or CS 1 or CS 104, and CS 103 or CS 104 or placement P6 first."), fixture(new int[] {1, 2, 1, 1},
						new int[][] {{1, 3, 102}, {1, 4, 2}, {3, 101}, {3, 102, 1}, {4, 3, 1, 104},
								{4, 103, 104}, {101, 103, 3}, {102, 4, 101, 100}, {103, 101, 104, 1}, {104, 4}},
						Map.of(1, List.of("P1"), 2, List.of("P2"), 6, List.of("P6")), 5, Map.of()));
	}

	@Test
	void omitsOptionAlreadyMetByEarlierRoadmapCourse() {
		assertEquals(List.of("7:101*", "7:104*", "8:4", "8:5", "9:2", "9:3", "10:1"), fixture(new int[] {4, 3, 3, 1, 1},
				new int[][] {{1, 100, 4}, {1, 104}, {2, 103, 101, 4}, {4, 100, 101, 2}, {4, 102, 104}, {5, 102, 101}, {5, 101}},
				Map.of(3, List.of("P3"), 4, List.of("P4")), 5, Map.of()));
	}

	@Test
	void prefersOneSharedOptionOverTwoOptionsOrAChain() {
		assertEquals(List.of("7:12*", "9:1", "9:4"), suggestions(List.of(required(1, 1), required(4, 1)),
				List.of(group(1, 3, 10, 12), group(4, 2, 12), group(10, 11)), List.of(), Map.of(), Map.of(List.of(7, 12), "High")));
	}

	@Test
	void searchesTwelveReachableOptions() {
		assertEquals(List.of("7:100*", "9:1"), planWithOptions(12));
	}

	@Test
	void leavesCourseUnplacedWithThirteenReachableOptions() {
		assertEquals(List.of("1:Needs CS 100 or CS 101 or CS 102 or CS 103 or CS 104 or CS 105 or CS 106 or CS 107 or "
				+ "CS 108 or CS 109 or CS 110 or CS 111 or CS 112 first."), planWithOptions(13));
	}

	@Test
	void prefersLowerRiskThenLowerNumberForEqualSizeSets() {
		List<RoadmapCourse> courses = List.of(required(1, 1));
		List<PrerequisiteGroup> groups = List.of(group(1, 11, 10));
		assertEquals(List.of("7:11*", "9:1"), suggestions(courses, groups, List.of(), Map.of(), Map.of(List.of(7, 10), "High")));
		assertEquals(List.of("7:10*", "9:1"), suggestions(courses, groups, List.of(), Map.of(), Map.of()));
	}

	@Test
	void leavesSavedRoadmapCourseOptionsOutOfSearch() {
		assertEquals(List.of("7:112*", "8:1"), schedule(
				List.of(required(1, 1), course(2, 1, "Major", false, "Fall 2026")),
				List.of(group(1, 112), outsideOptions(2)), List.of(planned(2, 20264)),
				Map.of(), outsideCatalog(13), Map.of(), consecutiveTerms(3)));
	}

	@Test
	void leavesOptionsForRoadmapCoursesWithoutAvailableTermsOutOfSearch() {
		assertEquals(List.of("7:112*", "9:1", "2:Roadmap gives CS 2 no term."), schedule(
				List.of(required(1, 1), required(2, 0)), List.of(group(1, 112), outsideOptions(2)),
				List.of(), Map.of(), outsideCatalog(13), Map.of()));
		assertEquals(List.of("7:112*", "9:1", "2:CS 2 has no term available yet."), schedule(
				List.of(required(1, 1), required(2, 4)), List.of(group(1, 112), outsideOptions(2)),
				List.of(), Map.of(), outsideCatalog(13), Map.of()));
	}

	@Test
	void comparesCourseNumbersAcrossEqualSizeSets() {
		Map<Integer, TranscriptCourse> options = catalog();
		options.put(13, new TranscriptCourse(13, "CS", "13", "", "3", null, true, false, false));
		assertEquals(List.of("7:10*", "7:13*", "8:1"), schedule(List.of(required(1, 1)),
				List.of(group(1, 10, 11), extraGroup(5, 12, 13), group(12, 11)), List.of(),
				Map.of(), options, Map.of(), consecutiveTerms(3)));
	}

	@Test
	void excludesPlacementOnlyOptionsFromSearchLimit() {
		assertEquals(List.of("7:100*", "9:1"), schedule(List.of(required(1, 1)),
				List.of(outsideOptions(1), extraGroup(5, 100, 112)), List.of(),
				Map.of(5, List.of("M-I")), outsideCatalog(13), Map.of()));
	}

	private PrerequisiteGroup outsideOptions(int owner) {
		PrerequisiteGroup group = new PrerequisiteGroup(owner, owner);
		for (int id = 100; id < 112; id++) {
			group.addOption(id, "CS " + id);
		}
		return group;
	}

	private List<String> planWithOptions(int count) {
		PrerequisiteGroup group = new PrerequisiteGroup(1, 1);
		for (int id = 100; id < 100 + count; id++) {
			group.addOption(id, "CS " + id);
		}
		return schedule(List.of(required(1, 1)), List.of(group), List.of(), Map.of(), outsideCatalog(count), Map.of());
	}

	private List<String> fixture(int[] positions, int[][] options, Map<Integer, List<String>> placements,
			int terms, Map<List<Integer>, String> risks) {
		List<RoadmapCourse> courses = new ArrayList<>();
		for (int i = 0; i < positions.length; i++) {
			courses.add(required(i + 1, positions[i]));
		}
		return schedule(courses, fixtureGroups(options), List.of(), placements, outsideCatalog(5), risks, consecutiveTerms(terms));
	}

	private List<SessionInfo> consecutiveTerms(int terms) {
		List<SessionInfo> open = new ArrayList<>();
		for (int i = 0; i < terms; i++) {
			open.add(new SessionInfo(100 + i, 7 + i, "Fall", "Fall", "Fall", 20264 + i));
		}
		return open;
	}

	private List<PrerequisiteGroup> fixtureGroups(int[][] rows) {
		List<PrerequisiteGroup> groups = new ArrayList<>();
		for (int i = 0; i < rows.length; i++) {
			PrerequisiteGroup group = new PrerequisiteGroup(i + 1, rows[i][0]);
			for (int j = 1; j < rows[i].length; j++) {
				int id = rows[i][j];
				group.addOption(id, "CS " + id);
			}
			groups.add(group);
		}
		return groups;
	}

	private Map<Integer, TranscriptCourse> outsideCatalog(int count) {
		Map<Integer, TranscriptCourse> catalog = new HashMap<>();
		for (int id = 100; id < 100 + count; id++) {
			catalog.put(id, new TranscriptCourse(id, "CS", String.valueOf(id), "", "3", null, true, false, false));
		}
		return catalog;
	}

	private List<String> suggestions(List<RoadmapCourse> courses, List<PrerequisiteGroup> groups,
			List<PlannedCourse> saved, Map<Integer, List<String>> placements, Map<List<Integer>, String> risks) {
		return schedule(courses, groups, saved, placements, catalog(), risks);
	}

	private Map<Integer, TranscriptCourse> catalog() {
		Map<Integer, TranscriptCourse> catalog = new HashMap<>();
		for (int id : List.of(2, 3, 10, 11, 12)) {
			catalog.put(id, new TranscriptCourse(id, "CS", String.valueOf(id), "", "3", null, true, false, false));
		}
		return catalog;
	}

	private List<String> schedule(List<RoadmapCourse> courses, List<PrerequisiteGroup> groups,
			List<PlannedCourse> saved, Map<Integer, List<String>> placements) {
		return schedule(courses, groups, saved, placements, Map.of(), Map.of());
	}

	private List<String> schedule(List<RoadmapCourse> courses, List<PrerequisiteGroup> groups,
			List<PlannedCourse> saved, Map<Integer, List<String>> placements,
			Map<Integer, TranscriptCourse> catalog, Map<List<Integer>, String> risks) {
		return schedule(courses, groups, saved, placements, catalog, risks, sessions());
	}

	private List<String> schedule(List<RoadmapCourse> courses, List<PrerequisiteGroup> groups, List<PlannedCourse> saved,
			Map<Integer, List<String>> placements, Map<Integer, TranscriptCourse> catalog, Map<List<Integer>, String> risks,
			List<SessionInfo> sessions) {
		GeneratedPlan plan = checkedPlan(courses, sessions, saved, groups, placements, catalog, risks);
		List<String> results = new ArrayList<>();
		for (ProposedTerm term : plan.getTerms()) {
			for (ProposedEntry entry : term.getEntries()) {
				String row = term.getTermId() + ":" + entry.getCourseId();
				if (entry.isSuggested()) {
					row = row + "*";
				}
				results.add(row);
			}
		}
		for (UnplacedCourse course : plan.getUnplaced()) {
			results.add(course.getCourseId() + ":" + course.getReason());
		}
		return results;
	}

	private GeneratedPlan checkedPlan(List<RoadmapCourse> courses, List<SessionInfo> sessions, List<PlannedCourse> saved,
			List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements, Map<Integer, TranscriptCourse> catalog,
			Map<List<Integer>, String> risks) {
		GeneratedPlan plan = PlanGenerator.mapCourses(courses, sessions, saved, groups, placements, catalog, risks);
		assertEquals(List.of(), checkPlan(plan, courses, sessions, saved, groups, placements));
		BestPlacement best = bestPlacement(courses, sessions, saved, groups, placements, catalog);
		int placed = 0;
		for (ProposedTerm term : plan.getTerms()) {
			for (ProposedEntry entry : term.getEntries()) {
				if (best.candidateCount() > 12) {
					assertFalse(entry.isSuggested());
				}
				if (!entry.isSuggested()) {
					placed++;
				}
			}
		}
		if (best.candidateCount() <= 12) {
			assertEquals(best.roadmapCount(), placed);
		}
		return plan;
	}

	static List<String> checkPlan(GeneratedPlan plan, List<RoadmapCourse> courses, List<SessionInfo> sessions,
			List<PlannedCourse> saved, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		List<String> problems = new ArrayList<>();
		List<PlannedCourse> proposed = new ArrayList<>();
		List<RoadmapCourse> suggestions = new ArrayList<>();
		for (ProposedTerm term : plan.getTerms()) {
			for (ProposedEntry entry : term.getEntries()) {
				proposed.add(new PlannedCourse(0, entry.getCourseId(), entry.getCode(), entry.getTitle(),
						entry.getUnits(), term.getTermId(), term.getLabel(), "", term.getTermOrder(), null));
				if (entry.isSuggested()) {
					String[] code = entry.getCode().split(" ", 2);
					suggestions.add(new RoadmapCourse(entry.getCourseId(), code[0], code[1], entry.getTitle(),
							String.valueOf(entry.getUnits()), 1, "Suggested", false, null));
				}
			}
		}
		List<PlannedCourse> prior = priorCourses(courses, saved);
		List<RoadmapCourse> pending = new ArrayList<>();
		int earliest = Integer.MAX_VALUE;
		for (RoadmapCourse course : courses) {
			if (!course.isPassed() && !"Elective".equals(course.getRequirementType())
					&& course.getPlannedTermLabel() == null) {
				pending.add(course);
				if (course.getSuggestedTerm() > 0) {
					earliest = Math.min(earliest, course.getSuggestedTerm());
				}
			}
		}
		checkTermLoads(plan, saved, problems);
		if (!validProposals(proposed, prior, groups, placements)) {
			problems.add("Invalid prerequisite order");
		}
		for (RoadmapCourse suggestion : suggestions) {
			if (!hasUse(suggestion.getCourseId(), proposed, prior, groups, placements)) {
				problems.add("No course-only use for " + suggestion.getCode());
			}
			List<RoadmapCourse> kept = new ArrayList<>(suggestions);
			kept.remove(suggestion);
			boolean changed;
			do {
				PlanGenerator.Placement run = new PlanGenerator.Placement(pending, earliest, sessions, prior,
						groups, placements, kept);
				List<PlannedCourse> replayed = replayedProposals(run, prior.size());
				changed = false;
				for (RoadmapCourse option : new ArrayList<>(kept)) {
					if (!hasUse(option.getCourseId(), replayed, prior, groups, placements)) {
						kept.remove(option);
						changed = true;
					}
				}
				if (!changed && validProposals(replayed, prior, groups, placements)
						&& replayed.size() - kept.size() >= proposed.size() - suggestions.size()) {
					problems.add("Removable " + suggestion.getCode());
				}
			} while (changed);
		}
		return problems;
	}

	private static void checkTermLoads(GeneratedPlan plan, List<PlannedCourse> saved, List<String> problems) {
		Map<Integer, Double> savedUnits = new HashMap<>();
		for (PlannedCourse course : saved) {
			int termId = course.getTermId();
			double units = savedUnits.getOrDefault(termId, 0.0);
			savedUnits.put(termId, units + course.getUnits());
		}
		for (ProposedTerm term : plan.getTerms()) {
			double units = savedUnits.getOrDefault(term.getTermId(), 0.0);
			for (ProposedEntry entry : term.getEntries()) {
				units += entry.getUnits();
			}
			if (!term.getEntries().isEmpty() && units > UnitRules.NORMAL_LIMIT) {
				problems.add(term.getLabel() + " exceeds the unit limit: " + units);
			}
		}
	}

	private static List<PlannedCourse> priorCourses(List<RoadmapCourse> courses, List<PlannedCourse> saved) {
		List<PlannedCourse> prior = new ArrayList<>(saved);
		for (RoadmapCourse course : courses) {
			if (course.isPassed()) {
				prior.add(new PlannedCourse(0, course.getCourseId(), course.getCode(), course.getTitle(),
						0, 0, "", "", Integer.MIN_VALUE, null));
			}
		}
		return prior;
	}

	private static boolean validProposals(List<PlannedCourse> proposed, List<PlannedCourse> prior,
			List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		List<PlannedCourse> all = new ArrayList<>(prior);
		all.addAll(proposed);
		PrerequisiteCheck check = new PrerequisiteCheck();
		for (PlannedCourse course : proposed) {
			if (!check.findMissing(course.getCourseId(), course.getTermOrder(), groups, all, placements).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasUse(int id, List<PlannedCourse> proposed, List<PlannedCourse> prior,
			List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements) {
		List<PlannedCourse> others = new ArrayList<>(prior);
		int order = Integer.MAX_VALUE;
		for (PlannedCourse course : proposed) {
			if (course.getCourseId() == id) {
				order = course.getTermOrder();
			} else {
				others.add(course);
			}
		}
		PrerequisiteCheck check = new PrerequisiteCheck();
		for (PrerequisiteGroup group : groups) {
			if (!group.hasOption(id) || !placements.getOrDefault(group.getGroupId(), List.of()).isEmpty()) {
				continue;
			}
			for (PlannedCourse owner : proposed) {
				if (owner.getCourseId() == group.getCourseId() && order < owner.getTermOrder()
						&& !check.findMissing(owner.getCourseId(), owner.getTermOrder(), List.of(group), others,
								placements).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private static List<PlannedCourse> replayedProposals(PlanGenerator.Placement run, int savedCount) {
		List<PlannedCourse> entries = run.plannedCourses();
		return entries.subList(savedCount, entries.size());
	}

	static BestPlacement bestPlacement(List<RoadmapCourse> courses, List<SessionInfo> sessions,
			List<PlannedCourse> saved, List<PrerequisiteGroup> groups, Map<Integer, List<String>> placements,
			Map<Integer, TranscriptCourse> catalog) {
		BestPlacement best = new BestPlacement(sessions, priorCourses(courses, saved), groups, placements);
		Set<Integer> reached = new HashSet<>();
		List<Integer> toVisit = new ArrayList<>();
		for (RoadmapCourse course : courses) {
			reached.add(course.getCourseId());
			if (!course.isPassed() && !"Elective".equals(course.getRequirementType())
					&& course.getPlannedTermLabel() == null) {
				best.pending.add(course);
				if (course.getSuggestedTerm() > 0) {
					best.earliest = Math.min(best.earliest, course.getSuggestedTerm());
				}
			}
		}
		for (RoadmapCourse course : best.pending) {
			int position = course.getSuggestedTerm();
			if (position > 0 && position - best.earliest < sessions.size()) {
				toVisit.add(course.getCourseId());
			}
		}
		for (PlannedCourse course : saved) {
			reached.add(course.getCourseId());
		}
		for (int i = 0; i < toVisit.size(); i++) {
			int owner = toVisit.get(i);
			for (PrerequisiteGroup group : groups) {
				if (group.getCourseId() != owner || !placements.getOrDefault(group.getGroupId(), List.of()).isEmpty()) {
					continue;
				}
				for (int id : group.getOptionCourseIds()) {
					TranscriptCourse option = catalog.get(id);
					if (reached.contains(id) || option == null || option.isPassed()) {
						continue;
					}
					reached.add(id);
					toVisit.add(id);
					String[] code = option.getCode().split(" ", 2);
					best.candidates.add(new RoadmapCourse(id, code[0], code[1], option.getTitle(), option.getUnits(),
							1, "Suggested", false, null));
				}
			}
		}
		if (best.candidateCount() <= 16) {
			best.trySubsets(0, new ArrayList<>());
		}
		return best;
	}

	static final class BestPlacement {
		private final List<SessionInfo> sessions;
		private final List<PlannedCourse> saved;
		private final List<PrerequisiteGroup> groups;
		private final Map<Integer, List<String>> placements;
		private final List<RoadmapCourse> pending = new ArrayList<>();
		private final List<RoadmapCourse> candidates = new ArrayList<>();
		private int earliest = Integer.MAX_VALUE;
		private int roadmapCount = -1;

		BestPlacement(List<SessionInfo> sessions, List<PlannedCourse> saved, List<PrerequisiteGroup> groups,
				Map<Integer, List<String>> placements) {
			this.sessions = sessions;
			this.saved = saved;
			this.groups = groups;
			this.placements = placements;
		}

		int candidateCount() {
			return candidates.size();
		}

		int roadmapCount() {
			return roadmapCount;
		}

		private void trySubsets(int next, List<RoadmapCourse> chosen) {
			if (next < candidates.size()) {
				trySubsets(next + 1, chosen);
				List<RoadmapCourse> included = new ArrayList<>(chosen);
				included.add(candidates.get(next));
				trySubsets(next + 1, included);
				return;
			}
			PlanGenerator.Placement run = new PlanGenerator.Placement(pending, earliest, sessions, saved,
					groups, placements, chosen);
			List<PlannedCourse> proposed = replayedProposals(run, saved.size());
			if (!validProposals(proposed, saved, groups, placements)) {
				return;
			}
			for (RoadmapCourse option : chosen) {
				if (!hasUse(option.getCourseId(), proposed, saved, groups, placements)) {
					return;
				}
			}
			roadmapCount = Math.max(roadmapCount, proposed.size() - chosen.size());
		}
	}

	@Test
	void movesCourseWhenSavedCoursesLeaveTooFewUnits() {
		assertEquals(List.of("9:1"), capSchedule(List.of(withUnits(1, 4)), savedLoads(16), List.of()));
	}

	@Test
	void leavesTermOverLimitWithOnlyItsSavedCourses() {
		assertEquals(List.of("9:1"), capSchedule(List.of(withUnits(1, 1)), savedLoads(20), List.of()));
	}

	@Test
	void placesFirstCourseAtLimitAndMovesNextCourse() {
		assertEquals(List.of("7:1", "9:2"), capSchedule(List.of(withUnits(2, 1), withUnits(1, 3)),
				savedLoads(16), List.of()));
	}

	@Test
	void placesDependentAfterPrerequisiteMoves() {
		assertEquals(List.of("9:1", "11:2"), capSchedule(List.of(withUnits(1, 4), required(2, 1)),
				savedLoads(16), List.of(group(2, 1))));
	}

	@Test
	void movesSuggestionAndCountsItsUnits() {
		assertEquals(List.of("7:1", "9:10*", "11:4"), suggestions(List.of(required(1, 1), required(4, 1)),
				List.of(group(4, 10)), savedLoads(16), Map.of(), Map.of()));
	}

	@Test
	void reportsNoRoomAndLeavesDependentWithPrerequisiteReason() {
		assertEquals(List.of("1:No room under 19 units by Fall 2027.", "2:Needs CS 1 first."),
				capSchedule(List.of(withUnits(1, 4), required(2, 1)), savedLoads(16, 16, 16), List.of(group(2, 1))));
	}

	@Test
	void reportsNoRoomForCourseOverLimitOnItsOwn() {
		assertEquals(List.of("1:No room under 19 units by Fall 2027."),
				capSchedule(List.of(withUnits(1, 20)), List.of(), List.of()));
	}

	@Test
	void choosesSuggestionThatLetsBothRoadmapCoursesFit() {
		Map<Integer, TranscriptCourse> options = catalog();
		options.put(11, new TranscriptCourse(11, "CS", "11", "", "2", null, true, false, false));
		assertEquals(List.of("7:11*", "9:1", "11:2"), schedule(List.of(required(1, 1), required(2, 1)),
				List.of(group(1, 10, 11), group(2, 1)), savedLoads(17), Map.of(), options, Map.of()));
	}

	private List<String> capSchedule(List<RoadmapCourse> courses, List<PlannedCourse> saved,
			List<PrerequisiteGroup> groups) {
		return schedule(courses, groups, saved, Map.of());
	}

	private RoadmapCourse withUnits(int id, int units) {
		return new RoadmapCourse(id, "CS", String.valueOf(id), "Course " + id, String.valueOf(units),
				1, "Major", false, null);
	}

	private List<PlannedCourse> savedLoads(int... totals) {
		return savedLoads(sessions(), totals);
	}

	private List<PlannedCourse> savedLoads(List<SessionInfo> terms, int... totals) {
		List<PlannedCourse> saved = new ArrayList<>();
		int id = 200;
		for (int i = 0; i < totals.length; i++) {
			SessionInfo term = terms.get(i);
			int unitsLeft = totals[i];
			while (unitsLeft > 0) {
				int units = Math.min(5, unitsLeft);
				saved.add(new PlannedCourse(0, id, "CS " + id, "", units, term.getTermId(), term.getTermLabel(),
						term.getSeason(), term.getTermOrder(), null));
				unitsLeft -= units;
				id++;
			}
		}
		return saved;
	}

	@Test
	void findsBestLegalPlanAcrossRoadmapRowsAndSearchSizes() {
		Random random = new Random(20261118);
		for (int index = 0; index < 2000; index++) {
			checkRoadmapCase(randomRoadmap(random), index);
		}
		for (int index = 0; index < 20; index++) {
			checkRoadmapCase(wideRoadmap(random, 9 + index % 5), 2000 + index);
		}
		Random capRandom = new Random(20261123);
		for (int index = 0; index < 300; index++) {
			checkRoadmapCase(capRoadmap(capRandom), 2020 + index);
		}
	}

	private SweepRoadmap randomRoadmap(Random random) {
		int termCount = 3 + random.nextInt(3);
		int courseCount = 3 + random.nextInt(4);
		SweepRoadmap input = new SweepRoadmap(consecutiveTerms(termCount), outsideCatalog(5));
		List<Integer> options = new ArrayList<>();
		Set<Integer> passed = new HashSet<>();
		for (int id = 1; id <= courseCount; id++) {
			int position = 1 + random.nextInt(termCount - 1);
			int kind = random.nextInt(8);
			String type = "Major";
			String savedTerm = null;
			if (kind == 0) {
				input.saved.add(savedCourse(id, input.sessions.get(random.nextInt(termCount))));
				savedTerm = "Fall";
			} else if (kind == 1) {
				type = "Elective";
			} else if (kind == 2) {
				passed.add(id);
			} else if (kind == 3) {
				position = 0;
			} else if (kind == 4) {
				position = termCount + 2;
			}
			input.courses.add(course(id, position, type, passed.contains(id), savedTerm));
			options.add(id);
		}
		for (int id = 100; id < 105; id++) {
			options.add(id);
		}
		for (int owner : options) {
			int groupCount = random.nextInt(3);
			if (owner >= 100) {
				groupCount = random.nextInt(2);
			}
			for (int index = 0; index < groupCount; index++) {
				int groupId = input.groups.size() + 1;
				PrerequisiteGroup group = new PrerequisiteGroup(groupId, owner);
				int optionCount = 1 + random.nextInt(3);
				Set<Integer> chosen = new HashSet<>();
				for (int i = 0; i < optionCount; i++) {
					int option = options.get(random.nextInt(options.size()));
					if (option != owner && !chosen.contains(option)) {
						chosen.add(option);
						group.addOption(option, "CS " + option);
					}
				}
				if (!Collections.disjoint(passed, group.getOptionCourseIds())) {
					continue;
				}
				input.groups.add(group);
				if (random.nextInt(8) == 0) {
					input.placements.put(groupId, List.of("M-I"));
				}
			}
		}
		if (random.nextBoolean()) {
			input.saved.add(savedCourse(104, input.sessions.get(termCount - 1)));
			input.risks.put(List.of(7, 101), "High");
		}
		return input;
	}

	private SweepRoadmap wideRoadmap(Random random, int candidates) {
		int termCount = 3 + random.nextInt(3);
		SweepRoadmap input = new SweepRoadmap(consecutiveTerms(termCount), outsideCatalog(candidates + 7));
		input.courses.add(required(1, 1));
		input.courses.add(course(2, 2, "Major", false, "Fall"));
		input.courses.add(course(3, 2, "Elective", false, null));
		input.courses.add(required(4, 0));
		input.courses.add(required(5, termCount + 1));
		input.courses.add(course(6, 1, "Major", true, null));
		SessionInfo last = input.sessions.get(termCount - 1);
		input.saved.add(savedCourse(2, last));
		int savedOption = 100 + candidates;
		input.saved.add(savedCourse(savedOption, last));
		PrerequisiteGroup options = new PrerequisiteGroup(1, 1);
		for (int id = 100; id < savedOption - 1; id++) {
			options.addOption(id, "CS " + id);
		}
		options.addOption(savedOption, "CS " + savedOption);
		input.groups.add(options);
		input.groups.add(group(100, savedOption - 1));
		for (int owner = 2; owner <= 6; owner++) {
			input.groups.add(group(owner, savedOption + owner - 1));
		}
		input.groups.add(extraGroup(7, 101, savedOption + 6));
		input.placements.put(7, List.of("M-I"));
		input.risks.put(List.of(7, 101), "High");
		return input;
	}

	private SweepRoadmap capRoadmap(Random random) {
		int terms = 3 + random.nextInt(3);
		SweepRoadmap input = new SweepRoadmap(consecutiveTerms(terms), Map.of());
		int courses = 3 + random.nextInt(4);
		for (int id = 1; id <= courses; id++) {
			input.courses.add(withUnits(id, 1 + random.nextInt(5)));
		}
		int[] loads = new int[terms];
		for (int i = 0; i < terms; i++) {
			loads[i] = 13 + random.nextInt(7);
		}
		input.saved.addAll(savedLoads(input.sessions, loads));
		return input;
	}

	private PlannedCourse savedCourse(int id, SessionInfo term) {
		return new PlannedCourse(0, id, "CS " + id, "", 3, term.getTermId(), term.getTermLabel(),
				term.getSeason(), term.getTermOrder(), null);
	}

	private void checkRoadmapCase(SweepRoadmap input, int index) {
		try {
			checkedPlan(input.courses, input.sessions, input.saved, input.groups, input.placements,
					input.catalog, input.risks);
		} catch (AssertionError | RuntimeException failure) {
			throw new AssertionError("Roadmap " + index + ": " + input.describe(), failure);
		}
	}

	private static final class SweepRoadmap {
		private final List<RoadmapCourse> courses = new ArrayList<>();
		private final List<PlannedCourse> saved = new ArrayList<>();
		private final List<PrerequisiteGroup> groups = new ArrayList<>();
		private final Map<Integer, List<String>> placements = new HashMap<>();
		private final Map<List<Integer>, String> risks = new HashMap<>();
		private final List<SessionInfo> sessions;
		private final Map<Integer, TranscriptCourse> catalog;

		SweepRoadmap(List<SessionInfo> sessions, Map<Integer, TranscriptCourse> catalog) {
			this.sessions = sessions;
			this.catalog = catalog;
		}

		String describe() {
			List<String> rows = new ArrayList<>();
			for (RoadmapCourse course : courses) {
				rows.add(course.getCode() + "@" + course.getSuggestedTerm() + " " + course.getRequirementType()
						+ " passed=" + course.isPassed() + " saved=" + course.getPlannedTermLabel());
			}
			List<String> needs = new ArrayList<>();
			for (PrerequisiteGroup group : groups) {
				needs.add(group.getCourseId() + " needs " + group.getOptionCodes()
						+ " placement=" + placements.get(group.getGroupId()));
			}
			List<String> savedRows = new ArrayList<>();
			for (PlannedCourse course : saved) {
				savedRows.add(course.getCode() + "@" + course.getTermId());
			}
			return rows + ", " + sessions.size() + " terms, groups=" + needs + ", outside=" + catalog.keySet()
					+ ", saved=" + savedRows + ", risks=" + risks;
		}
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
	private List<SessionInfo> sessions(int count) {
		List<SessionInfo> terms = new ArrayList<>(sessions());
		terms.add(new SessionInfo(13, 12, "Spring", "Spring 2028", "Spring 2028", 20282));
		terms.add(new SessionInfo(15, 13, "Fall", "Fall 2028", "Fall 2028", 20284));
		return terms.subList(0, count);
	}
}
