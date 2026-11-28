package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

import com.coursecraft.service.UnitRules;

// holds one elective rule's minimums and student's current progress (e.g. "1 of 1 course, 6 of 17 units")
public class ElectiveGroupStatus {

	private final int groupId;
	private final String name;
	private final Double minUnits;
	private final Integer minCourses;
	private final double doneUnits;
	private final int doneCourses;

	public ElectiveGroupStatus(int groupId, String name, Double minUnits, Integer minCourses,
			double doneUnits, int doneCourses) {
		this.groupId = groupId;
		this.name = name;
		this.minUnits = minUnits;
		this.minCourses = minCourses;
		this.doneUnits = doneUnits;
		this.doneCourses = doneCourses;
	}

	public int getGroupId() {
		return groupId;
	}

	public String getName() {
		return name;
	}

	public boolean isSatisfied() {
		boolean unitsOk = minUnits == null || doneUnits + 0.001 >= minUnits;
		boolean coursesOk = minCourses == null || doneCourses >= minCourses;
		return unitsOk && coursesOk;
	}

	public String getProgressText() {
		List<String> parts = new ArrayList<>();
		if (minCourses != null) {
			parts.add(doneCourses + " of " + minCourses + (minCourses == 1 ? " course" : " courses"));
		}
		if (minUnits != null) {
			parts.add(UnitRules.format(doneUnits) + " of " + UnitRules.format(minUnits) + " units");
		}
		return String.join(", ", parts);
	}
}