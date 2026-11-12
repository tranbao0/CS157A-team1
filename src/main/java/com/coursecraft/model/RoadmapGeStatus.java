package com.coursecraft.model;

// holds one required GE area from requires_ge_area with how it was met (course code, placement, or null if remaining)
public class RoadmapGeStatus {

	private final String code;
	private final String name;
	private final String fulfilledByCode;
	private final boolean hasCourses;

	public RoadmapGeStatus(String code, String name, String fulfilledByCode, boolean hasCourses) {
		this.code = code;
		this.name = name;
		this.fulfilledByCode = fulfilledByCode;
		this.hasCourses = hasCourses;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	// passed course code (e.g. "ENGL 1A"), placement label (e.g. "Placement (W-I)"), or null if unfulfilled
	public String getFulfilledByCode() {
		return fulfilledByCode;
	}

	public boolean isFulfilled() {
		return fulfilledByCode != null;
	}

	// false for areas like 5C that have no standalone courses in the catalog
	public boolean isHasCourses() {
		return hasCourses;
	}
}