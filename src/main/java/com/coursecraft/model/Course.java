package com.coursecraft.model;

// holds one row of courses table, only columns plan add needs for now
public class Course {

	private final int courseId;
	private final String code;
	private final double units;

	public Course(int courseId, String code, double units) {
		this.courseId = courseId;
		this.code = code;
		this.units = units;
	}

	public int getCourseId() {
		return courseId;
	}

	// gives short name like "CS 146"
	public String getCode() {
		return code;
	}

	public double getUnits() {
		return units;
	}
}
