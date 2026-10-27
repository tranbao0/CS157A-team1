package com.coursecraft.model;

// holds one terms row, profile grad term dropdown lists these
public class Term {

	private final int termId;
	private final String label;

	public Term(int termId, String label) {
		this.termId = termId;
		this.label = label;
	}

	public int getTermId() {
		return termId;
	}

	// gives text like "Spring 2030"
	public String getLabel() {
		return label;
	}
}
