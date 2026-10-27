package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

import com.coursecraft.service.UnitRules;

// holds one term of student's plan for plan card: its courses, unit total and warnings
public class PlanTerm {

	private final int termId;
	private final String label;
	private final String season;
	private final List<PlannedCourse> courses = new ArrayList<>();
	private final List<PlanWarning> warnings = new ArrayList<>();
	private double units;
	private boolean gradTerm;

	public PlanTerm(int termId, String label, String season) {
		this.termId = termId;
		this.label = label;
		this.season = season;
	}

	// adds course to this term and counts its units into term total
	public void addCourse(PlannedCourse course) {
		courses.add(course);
		units = units + course.getUnits();
	}

	public void addWarning(PlanWarning warning) {
		warnings.add(warning);
	}

	// marks term student picked as expected graduation on Profile, My plan draws gold star there
	public void markGradTerm() {
		gradTerm = true;
	}

	public boolean isGradTerm() {
		return gradTerm;
	}

	public int getTermId() {
		return termId;
	}

	public String getLabel() {
		return label;
	}

	public String getSeason() {
		return season;
	}

	public List<PlannedCourse> getCourses() {
		return courses;
	}

	public List<PlanWarning> getWarnings() {
		return warnings;
	}

	public double getUnits() {
		return units;
	}

	// gives units as text for JSP, "20" instead of "20.0"
	public String getUnitsText() {
		return UnitRules.format(units);
	}

	// gives status word like "full-time" or "petition", see UnitRules.statusFor
	public String getStatus() {
		return UnitRules.statusFor(season, units);
	}
}
