package com.coursecraft.model;

// holds one roadmaps row with its major name, profile major dropdown lists these
public class Roadmap {

	private final int roadmapId;
	private final String majorName;
	private final int catalogYear;

	public Roadmap(int roadmapId, String majorName, int catalogYear) {
		this.roadmapId = roadmapId;
		this.majorName = majorName;
		this.catalogYear = catalogYear;
	}

	public int getRoadmapId() {
		return roadmapId;
	}

	public String getMajorName() {
		return majorName;
	}

	public int getCatalogYear() {
		return catalogYear;
	}

	// gives dropdown text like "Computer Science, BS (2025)"
	public String getLabel() {
		return majorName + " (" + catalogYear + ")";
	}
}
