package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;

class PlanGenerateSaveTest {

	private final List<String> proposed = List.of("1:9:false", "2:9:true", "3:11:false");

	@Test
	void matchesSameEntriesInAnotherOrder() {
		assertTrue(PlanGenerateSave.sameEntries(List.of("3:11:false", "1:9:false", "2:9:true"), proposed));
	}

	@Test
	void rejectsExtraMissingChangedAndRepeatedEntries() {
		assertFalse(PlanGenerateSave.sameEntries(List.of("1:9:false", "2:9:true", "3:11:false", "4:9:false"), proposed));
		assertFalse(PlanGenerateSave.sameEntries(List.of("1:9:false", "2:9:true"), proposed));
		assertFalse(PlanGenerateSave.sameEntries(List.of("1:9:false", "2:11:true", "3:11:false"), proposed));
		assertFalse(PlanGenerateSave.sameEntries(List.of("1:9:false", "2:9:false", "3:11:false"), proposed));
		assertFalse(PlanGenerateSave.sameEntries(List.of("1:9:false", "1:9:false", "3:11:false"), proposed));
		assertFalse(PlanGenerateSave.sameEntries(List.of("1:9:false", "2:9:true", "3:11:false", "1:9:false"), proposed));
	}

	@Test
	void rejectsEmptyListsEvenWhenBothAreEmpty() {
		assertFalse(PlanGenerateSave.sameEntries(List.of(), List.of()));
		assertFalse(PlanGenerateSave.sameEntries(List.of(), proposed));
	}

	@Test
	void readsEntryKeysFromThePlan() {
		GeneratedPlan plan = new GeneratedPlan();
		ProposedTerm term = new ProposedTerm(7, "Fall 2026", "Fall", 20264);
		term.addEntry(new ProposedEntry(5, "CS 5", "Course 5", 3, 9, true));
		plan.getTerms().add(term);
		assertEquals(List.of("5:9:true"), PlanGenerateSave.entryKeys(plan));
	}

	@Test
	void parsesPostedEntriesAndRejectsMalformedOnes() {
		assertEquals(List.of("12:34:true", "5:6:false"), PlanGenerateSave.parseEntries(new String[] { "12:34:true", "5:6:false" }));
		assertNull(PlanGenerateSave.parseEntries(null));
		assertNull(PlanGenerateSave.parseEntries(new String[] {}));
		assertNull(PlanGenerateSave.parseEntries(new String[] { "abc" }));
		assertNull(PlanGenerateSave.parseEntries(new String[] { "1:2" }));
		assertNull(PlanGenerateSave.parseEntries(new String[] { "1:2:3:true" }));
		assertNull(PlanGenerateSave.parseEntries(new String[] { "x:2:true" }));
		assertNull(PlanGenerateSave.parseEntries(new String[] { "1:0:true" }));
		assertNull(PlanGenerateSave.parseEntries(new String[] { "1:2:yes" }));
		assertNull(PlanGenerateSave.parseEntries(new String[] { "1:2:true", "" }));
	}
}
