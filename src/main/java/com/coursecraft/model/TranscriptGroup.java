package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

/*
 * One category on transcript page: a GE area, or one of the no-GE groups.
 * done, total and metByCourses describe whole category, so search result that lists only some courses
 * still shows "2 of 9" or "Met by MATH 30".
 */
public class TranscriptGroup {

	private final String key;
	private final String title;
	private final String note;
	private final List<TranscriptCourse> courses;
	private final int done;
	private final int total;
	private final boolean geArea;
	private final String metByCourses;
	private final String metByPlacement;

	// non-GE category, heading counts passed courses like "2 of 9"
	public TranscriptGroup(String key, String title, String note, List<TranscriptCourse> courses) {
		this(key, title, note, courses, countPassed(courses), courses.size(), false, null, null);
	}

	// GE area, one passed course meets it, so heading names passed courses like "Met by MATH 30, MATH 31"
	public static TranscriptGroup geArea(String key, String title, String note, List<TranscriptCourse> courses) {
		List<String> passed = new ArrayList<>();
		for (TranscriptCourse course : courses) {
			if (course.isPassed()) {
				passed.add(course.getCode());
			}
		}
		return new TranscriptGroup(key, title, note, courses, passed.size(), courses.size(), true,
				passed.isEmpty() ? null : String.join(", ", passed), null);
	}

	private TranscriptGroup(String key, String title, String note, List<TranscriptCourse> courses, int done,
			int total, boolean geArea, String metByCourses, String metByPlacement) {
		this.key = key;
		this.title = title;
		this.note = note;
		this.courses = courses;
		this.done = done;
		this.total = total;
		this.geArea = geArea;
		this.metByCourses = metByCourses;
		this.metByPlacement = metByPlacement;
	}

	// same category with only some of its courses, counts and met text stay those of whole category
	public TranscriptGroup withCourses(List<TranscriptCourse> some) {
		return new TranscriptGroup(key, title, note, some, done, total, geArea, metByCourses, metByPlacement);
	}

	// same GE area already met by placement like "W-I", with or without a passed course
	public TranscriptGroup withPlacement(String placement) {
		return new TranscriptGroup(key, title, note, courses, done, total, geArea, metByCourses, placement);
	}

	private static int countPassed(List<TranscriptCourse> courses) {
		int passed = 0;
		for (TranscriptCourse course : courses) {
			if (course.isPassed()) {
				passed++;
			}
		}
		return passed;
	}

	// search dropdown value, like "ge-A1" or "prereq"
	public String getKey() {
		return key;
	}

	public String getTitle() {
		return title;
	}

	public String getNote() {
		return note;
	}

	public List<TranscriptCourse> getCourses() {
		return courses;
	}

	public int getDone() {
		return done;
	}

	public int getTotal() {
		return total;
	}

	// placement code that meets this GE area, like "M-I", null when no placement does
	public String getMetByPlacement() {
		return metByPlacement;
	}

	// tells if some course in category is not passed yet, so search still offers it.
	// Met GE area still counts here: a major can require MATH 31 from an Area 2 that MATH 30 already met.
	public boolean isHasCoursesLeft() {
		return done < total;
	}

	/*
	 * Shown next to category name. GE area: "Met by M-I placement", "Met by MATH 30" or "Not met yet",
	 * since one course meets it and "0 of 4" would read like all four are needed. Other category: "2 of 4".
	 */
	public String getStatusText() {
		if (metByPlacement != null) {
			return "Met by " + metByPlacement + " placement";
		}
		if (metByCourses != null) {
			return "Met by " + metByCourses;
		}
		if (geArea) {
			return "Not met yet";
		}
		return done + " of " + total;
	}
}
