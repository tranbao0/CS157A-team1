package com.coursecraft.model;

import java.util.List;

// holds student's roadmap progress, term by term grid, elective group rules, and GE area statuses for transcript page
public class RemainingAudit {

	private final Roadmap roadmap;
	private final List<RoadmapTerm> terms;
	private final List<ElectiveGroupStatus> electiveGroups;
	private final List<RoadmapGeStatus> geAreas;
	private final int doneCourses;
	private final int totalCourses;
	private final int doneGe;
	private final int totalGe;

	public RemainingAudit(Roadmap roadmap,
			List<RoadmapTerm> terms,
			List<ElectiveGroupStatus> electiveGroups,
			List<RoadmapGeStatus> geAreas,
			int doneCourses, int totalCourses,
			int doneGe, int totalGe) {
		this.roadmap = roadmap;
		this.terms = terms;
		this.electiveGroups = electiveGroups;
		this.geAreas = geAreas;
		this.doneCourses = doneCourses;
		this.totalCourses = totalCourses;
		this.doneGe = doneGe;
		this.totalGe = totalGe;
	}

	public Roadmap getRoadmap() {
		return roadmap;
	}

	public List<RoadmapTerm> getTerms() {
		return terms;
	}

	public List<ElectiveGroupStatus> getElectiveGroups() {
		return electiveGroups;
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

	public int getDoneGe() {
		return doneGe;
	}

	public int getTotalGe() {
		return totalGe;
	}
}