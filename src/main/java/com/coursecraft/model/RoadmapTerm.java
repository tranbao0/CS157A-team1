package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

import com.coursecraft.service.UnitRules;

// holds one suggested term bucket (Term 1 to Term 8) on the /roadmap page
public class RoadmapTerm {

	private final int termPosition;
	private final List<RoadmapCourse> courses = new ArrayList<>();
	private double units;

	public RoadmapTerm(int termPosition) {
		this.termPosition = termPosition;
	}

	public void addCourse(RoadmapCourse course) {
		courses.add(course);
		try {
			units += Double.parseDouble(course.getUnits());
		} catch (NumberFormatException ignored) {
			// keep units unchanged if non-numeric
		}
	}

	public int getTermPosition() {
		return termPosition;
	}

	public String getLabel() {
		return "Term " + termPosition;
	}

	public List<RoadmapCourse> getCourses() {
		return courses;
	}

	public String getUnitsText() {
		return UnitRules.format(units);
	}
}