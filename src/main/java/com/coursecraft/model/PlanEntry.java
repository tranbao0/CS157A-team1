package com.coursecraft.model;

// holds one plan_entries row, remove uses it to know which course and session go away
public class PlanEntry {

	private final int planEntryId;
	private final int courseId;
	private final int sessionId;

	public PlanEntry(int planEntryId, int courseId, int sessionId) {
		this.planEntryId = planEntryId;
		this.courseId = courseId;
		this.sessionId = sessionId;
	}

	public int getPlanEntryId() {
		return planEntryId;
	}

	public int getCourseId() {
		return courseId;
	}

	public int getSessionId() {
		return sessionId;
	}
}
