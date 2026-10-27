package com.coursecraft.model;

// holds one placement_categories row like M-II, kind is "Math" or "Writing"
public class PlacementCategory {

	private final String code;
	private final String kind;
	private final String name;

	public PlacementCategory(String code, String kind, String name) {
		this.code = code;
		this.kind = kind;
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public String getKind() {
		return kind;
	}

	public String getName() {
		return name;
	}
}
