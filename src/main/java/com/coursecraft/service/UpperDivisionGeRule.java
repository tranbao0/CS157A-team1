package com.coursecraft.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.coursecraft.model.PlannedCourse;

/*
 * SJSU rule for every upper division (UD) GE course: completion of core GE and upper division standing.
 * Core GE = lower division GE areas student's roadmap requires (every lower division area when roadmap lists none).
 * Upper division standing = 60 units, counting passed courses and courses planned in earlier terms.
 * Area is met by passed or earlier planned course in that area, or by placement (W-I -> 1A, M-I -> 2).
 * 5C is also met by 4-unit 5A or 5B lab course, same rule as RoadmapDao GE audit.
 * prerequisite_groups cannot hold this rule, so PrerequisiteCheck and PlanAvailability ask this class too.
 */
public class UpperDivisionGeRule {

	public static final double STANDING_UNITS = 60.0;

	// course ID -> GE area code, only courses that have one
	private final Map<Integer, String> courseAreas;
	// course ID -> units, every course
	private final Map<Integer, Double> courseUnits;
	private final Set<Integer> passedIds;
	private final Set<String> placementAreas;
	// TreeSet sorts area codes, so message lists 1A, 1B ... 6 in order
	private final Set<String> coreAreas;

	public UpperDivisionGeRule(Map<Integer, String> courseAreas, Map<Integer, Double> courseUnits,
			Set<Integer> passedIds, Set<String> placementAreas, Set<String> coreAreas) {
		this.courseAreas = courseAreas;
		this.courseUnits = courseUnits;
		this.passedIds = passedIds;
		this.placementAreas = placementAreas;
		this.coreAreas = new TreeSet<>(coreAreas);
	}

	// tells if course is UD GE course, its area code starts with UD
	public boolean appliesTo(int courseId) {
		String area = courseAreas.get(courseId);
		return area != null && area.startsWith("UD");
	}

	/*
	 * Lists what student still misses before course can go in target term, empty when met or rule does not apply.
	 * skipCourseId is left out of plan, plan remove passes course being removed. Pass 0 to skip nothing.
	 */
	public List<String> findMissing(int courseId, int targetTermOrder, List<PlannedCourse> planned,
			int skipCourseId) {
		List<String> missing = new ArrayList<>();
		if (!appliesTo(courseId)) {
			return missing;
		}

		double units = 0;
		Set<String> metAreas = new TreeSet<>(placementAreas);
		for (int passedId : passedIds) {
			units = units + unitsOf(passedId);
			addArea(metAreas, passedId);
		}
		for (PlannedCourse course : planned) {
			int id = course.getCourseId();
			// passed course already counted above, so it is not counted twice
			if (id != skipCourseId && course.getTermOrder() < targetTermOrder && !passedIds.contains(id)) {
				units = units + unitsOf(id);
				addArea(metAreas, id);
			}
		}

		if (units < STANDING_UNITS) {
			missing.add("upper division standing (60 units passed or planned before this term, "
					+ formatUnits(units) + " so far)");
		}

		List<String> openAreas = new ArrayList<>();
		for (String area : coreAreas) {
			if (!metAreas.contains(area)) {
				openAreas.add(area);
			}
		}
		if (openAreas.size() == 1) {
			missing.add("core GE Area " + openAreas.get(0));
		} else if (openAreas.size() > 1) {
			missing.add("core GE Areas " + String.join(", ", openAreas));
		}
		return missing;
	}

	/*
	 * Tells if removing planned course breaks UD GE course planned in later term.
	 * Only counts when later course meets rule now and stops meeting it without removed course.
	 */
	public boolean isBrokenByRemoving(int laterCourseId, int laterTermOrder, List<PlannedCourse> planned,
			int removedCourseId) {
		return findMissing(laterCourseId, laterTermOrder, planned, 0).isEmpty()
				&& !findMissing(laterCourseId, laterTermOrder, planned, removedCourseId).isEmpty();
	}

	private double unitsOf(int courseId) {
		Double units = courseUnits.get(courseId);
		if (units == null) {
			return 0;
		}
		return units;
	}

	// adds course's GE area, and 5C too when course is 4-unit 5A or 5B lab course
	private void addArea(Set<String> metAreas, int courseId) {
		String area = courseAreas.get(courseId);
		if (area == null) {
			return;
		}
		metAreas.add(area);
		if ((area.equals("5A") || area.equals("5B")) && unitsOf(courseId) >= 4.0) {
			metAreas.add("5C");
		}
	}

	// 45.0 shows as "45", 45.5 stays "45.5"
	private static String formatUnits(double units) {
		if (units == Math.floor(units)) {
			return String.valueOf((long) units);
		}
		return String.valueOf(units);
	}
}
