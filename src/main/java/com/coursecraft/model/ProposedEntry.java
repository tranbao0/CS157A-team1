package com.coursecraft.model;

public class ProposedEntry {

	private final int courseId;
	private final String code;
	private final double units;
	private final int sessionId;
	private final String termLabel;
	private final boolean suggested;

	public ProposedEntry(int courseId, String code, double units, int sessionId, String termLabel, boolean suggested) {
		this.courseId = courseId;
		this.code = code;
		this.units = units;
		this.sessionId = sessionId;
		this.termLabel = termLabel;
		this.suggested = suggested;
	}

	public int getCourseId() {
		return courseId;
	}

	public String getCode() {
		return code;
	}

	public double getUnits() {
		return units;
	}

	public int getSessionId() {
		return sessionId;
	}

	public String getTermLabel() {
		return termLabel;
	}

	public boolean isSuggested() {
		return suggested;
	}
}
