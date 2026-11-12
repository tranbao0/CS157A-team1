package com.coursecraft.model;

public class RoadmapCourse {

	private final int courseId;
	private final String subject;
	private final String courseNumber;
	private final String title;
	private final String units;
	private final int suggestedTerm;
	private final String requirementType;
	private final boolean passed;
	private final String plannedTermLabel;

	public RoadmapCourse(int courseId, String subject, String courseNumber, String title, String units,
			int suggestedTerm, String requirementType, boolean passed, String plannedTermLabel) {
		this.courseId = courseId;
		this.subject = subject;
		this.courseNumber = courseNumber;
		this.title = title;
		this.units = units;
		this.suggestedTerm = suggestedTerm;
		this.requirementType = requirementType;
		this.passed = passed;
		this.plannedTermLabel = plannedTermLabel;
	}

	public int getCourseId() {
		return courseId;
	}

	public String getCode() {
		return subject + " " + courseNumber;
	}

	public String getTitle() {
		return title;
	}

	public String getUnits() {
		return units;
	}

	public int getSuggestedTerm() {
		return suggestedTerm;
	}

	public String getRequirementType() {
		return requirementType;
	}

	public boolean isPassed() {
		return passed;
	}

	public String getPlannedTermLabel() {
		return plannedTermLabel;
	}
}