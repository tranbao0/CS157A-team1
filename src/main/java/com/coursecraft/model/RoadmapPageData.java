package com.coursecraft.model;

import java.util.List;

// holds full roadmap grid (grouped by suggested term) and GE fulfillment list for /roadmap
public class RoadmapPageData {

	private final Roadmap roadmap;
	private final List<RoadmapTerm> terms;
	private final List<RoadmapGeStatus> geAreas;
	private final int doneCourses;
	private final int totalCourses;
	private final int doneElectives;
	private final int totalElectives;
	private final int doneGe;
	private final int totalGe;

	public RoadmapPageData(Roadmap roadmap,
			List<RoadmapTerm> terms,
			List<RoadmapGeStatus> geAreas,
			int doneCourses, int totalCourses,
			int doneElectives, int totalElectives,
			int doneGe, int totalGe) {
		this.roadmap = roadmap;
		this.terms = terms;
		this.geAreas = geAreas;
		this.doneCourses = doneCourses;
		this.totalCourses = totalCourses;
		this.doneElectives = doneElectives;
		this.totalElectives = totalElectives;
		this.doneGe = doneGe;
		this.totalGe = totalGe;
	}

	public Roadmap getRoadmap() {
		return roadmap;
	}

	public List<RoadmapTerm> getTerms() {
		return terms;
	}

	public List<RoadmapGeStatus> getGeAreas() {
		return geAreas;
	}

	public int getDoneCourses() {
		return doneCourses;
	}

	public int getTotalCourses() {
		return totalCourses;
	}

	public int getDoneElectives() {
		return doneElectives;
	}

	public int getTotalElectives() {
		return totalElectives;
	}

	public int getDoneGe() {
		return doneGe;
	}

	public int getTotalGe() {
		return totalGe;
	}
}