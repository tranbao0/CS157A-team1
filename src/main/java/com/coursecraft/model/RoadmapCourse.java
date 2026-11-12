package com.coursecraft.model;

// holds one required course from requires_course with whether student has passed it
public class RoadmapCourse {

	private final int courseId;
	private final String subject;
	private final String courseNumber;
	private final String title;
	private final String units;
	private final int suggestedTerm;
	private final String requirementType;
	private final boolean passed;

	public RoadmapCourse(int courseId, String subject, String courseNumber, String title, String units,
			int suggestedTerm, String requirementType, boolean passed) {
		this.courseId = courseId;
		this.subject = subject;
		this.courseNumber = courseNumber;
		this.title = title;
		this.units = units;
		this.suggestedTerm = suggestedTerm;
		this.requirementType = requirementType;
		this.passed = passed;
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
}