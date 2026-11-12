package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

import com.coursecraft.service.UnitRules;

// holds one suggested term bucket (e.g. "Term 1") and its roadmap courses
public class RoadmapTerm {

	private final int termNumber;
	private final List<RoadmapCourse> courses = new ArrayList<>();
	private double units;

	public RoadmapTerm(int termNumber) {
		this.termNumber = termNumber;
	}

	public void addCourse(RoadmapCourse course) {
		courses.add(course);
		if (!"Elective".equalsIgnoreCase(course.getRequirementType())) {
			try {
				units += Double.parseDouble(course.getUnits());
			} catch (NumberFormatException ignored) {
			}
		}
	}

	public int getTermNumber() {
		return termNumber;
	}

	public String getLabel() {
		return termNumber > 0 ? "Term " + termNumber : "Unscheduled";
	}

	public List<RoadmapCourse> getCourses() {
		return courses;
	}

	public double getUnits() {
		return units;
	}

	public String getUnitsText() {
		return UnitRules.format(units);
	}
}