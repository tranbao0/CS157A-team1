package com.coursecraft.model;

public class ProposedEntry {

	private final int courseId;
	private final String code;
	private final double units;
	private final int sessionId;
	private final String title;
	private final boolean suggested;

	public ProposedEntry(int courseId, String code, String title, double units, int sessionId, boolean suggested) {
		this.courseId = courseId;
		this.code = code;
		this.units = units;
		this.sessionId = sessionId;
		this.title = title;
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

	public String getTitle() {
		return title;
	}

	public boolean isSuggested() {
		return suggested;
	}
}
