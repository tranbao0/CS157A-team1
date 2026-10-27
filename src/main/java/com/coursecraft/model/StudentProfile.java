package com.coursecraft.model;

/*
 * Holds what profile form shows and saves for one student.
 * Every field can be null, since sign-up leaves them empty until student fills form.
 * Math and writing category come from student_placements, rest from students row.
 */
public class StudentProfile {

	private final Integer roadmapId;
	private final Integer gradTermId;
	private final Integer ppaScore;
	private final String mathCategory;
	private final String writingCategory;

	public StudentProfile(Integer roadmapId, Integer gradTermId, Integer ppaScore, String mathCategory,
			String writingCategory) {
		this.roadmapId = roadmapId;
		this.gradTermId = gradTermId;
		this.ppaScore = ppaScore;
		this.mathCategory = mathCategory;
		this.writingCategory = writingCategory;
	}

	public Integer getRoadmapId() {
		return roadmapId;
	}

	public Integer getGradTermId() {
		return gradTermId;
	}

	public Integer getPpaScore() {
		return ppaScore;
	}

	public String getMathCategory() {
		return mathCategory;
	}

	public String getWritingCategory() {
		return writingCategory;
	}
}
