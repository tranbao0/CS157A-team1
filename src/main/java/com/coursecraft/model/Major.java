package com.coursecraft.model;

// holds one majors row, transcript import matches transcript's plan line to these
public class Major {

	private final int majorId;
	private final String name;

	public Major(int majorId, String name) {
		this.majorId = majorId;
		this.name = name;
	}

	public int getMajorId() {
		return majorId;
	}

	public String getName() {
		return name;
	}
}
