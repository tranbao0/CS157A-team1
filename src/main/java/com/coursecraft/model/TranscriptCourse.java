package com.coursecraft.model;

// holds one course on transcript page, with flags that pick its category and whether student passed it
public class TranscriptCourse {

	private final int courseId;
	private final String subject;
	private final String courseNumber;
	private final String title;
	private final String units;
	private final String geAreaCode;
	private final boolean prerequisite;
	private final boolean hasPrerequisites;
	private final boolean passed;

	public TranscriptCourse(int courseId, String subject, String courseNumber, String title, String units,
			String geAreaCode, boolean prerequisite, boolean hasPrerequisites, boolean passed) {
		this.courseId = courseId;
		this.subject = subject;
		this.courseNumber = courseNumber;
		this.title = title;
		this.units = units;
		this.geAreaCode = geAreaCode;
		this.prerequisite = prerequisite;
		this.hasPrerequisites = hasPrerequisites;
		this.passed = passed;
	}

	public int getCourseId() {
		return courseId;
	}

	// gives short name like "CS 146"
	public String getCode() {
		return subject + " " + courseNumber;
	}

	public String getTitle() {
		return title;
	}

	// text as MySQL gives it, so 3 units shows as "3"
	public String getUnits() {
		return units;
	}

	// null when course counts toward no GE area
	public String getGeAreaCode() {
		return geAreaCode;
	}

	// tells if some other course needs this one
	public boolean isPrerequisite() {
		return prerequisite;
	}

	public boolean isHasPrerequisites() {
		return hasPrerequisites;
	}

	public boolean isPassed() {
		return passed;
	}
}
