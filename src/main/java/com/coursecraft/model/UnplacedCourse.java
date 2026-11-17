package com.coursecraft.model;

public class UnplacedCourse {

	private final int courseId;
	private final String code;
	private final double units;
	private final String reason;

	public UnplacedCourse(int courseId, String code, double units, String reason) {
		this.courseId = courseId;
		this.code = code;
		this.units = units;
		this.reason = reason;
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

	public String getReason() {
		return reason;
	}
}
