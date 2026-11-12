package com.coursecraft.model;

// holds one required GE area from requires_ge_area with the passed course that met it (null if remaining)
public class RoadmapGeStatus {

	private final String code;
	private final String name;
	private final String fulfilledByCode;

	public RoadmapGeStatus(String code, String name, String fulfilledByCode) {
		this.code = code;
		this.name = name;
		this.fulfilledByCode = fulfilledByCode;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getFulfilledByCode() {
		return fulfilledByCode;
	}

	public boolean isFulfilled() {
		return fulfilledByCode != null;
	}
}