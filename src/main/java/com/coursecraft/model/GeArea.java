package com.coursecraft.model;

// holds one ge_areas row, like A1 Oral Communication
public class GeArea {

	private final String code;
	private final String name;

	public GeArea(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
