package com.coursecraft.model;

import java.util.List;

// holds student's roadmap progress, term-by-term grid, and remaining GE areas for transcript page
public class RemainingAudit {

	private final Roadmap roadmap;
	private final List<RoadmapTerm> terms;
	private final List<RoadmapGeStatus> remainingGeAreas;
	private final int doneCourses;
	private final int totalCourses;
	private final int doneElectives;
	private final int totalElectives;
	private final int doneGe;
	private final int totalGe;

	public RemainingAudit(Roadmap roadmap,
			List<RoadmapTerm> terms,
			List<RoadmapGeStatus> remainingGeAreas,
			int doneCourses, int totalCourses,
			int doneElectives, int totalElectives,
			int doneGe, int totalGe) {
		this.roadmap = roadmap;
		this.terms = terms;
		this.remainingGeAreas = remainingGeAreas;
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

	public List<RoadmapGeStatus> getRemainingGeAreas() {
		return remainingGeAreas;
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