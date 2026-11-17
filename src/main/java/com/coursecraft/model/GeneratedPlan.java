package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

public class GeneratedPlan {

	private final List<ProposedTerm> terms = new ArrayList<>();
	private final List<UnplacedCourse> unplaced = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	public List<ProposedTerm> getTerms() {
		return terms;
	}

	public List<UnplacedCourse> getUnplaced() {
		return unplaced;
	}

	public List<String> getWarnings() {
		return warnings;
	}
}
