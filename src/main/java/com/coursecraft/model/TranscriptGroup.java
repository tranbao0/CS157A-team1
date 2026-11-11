package com.coursecraft.model;

import java.util.List;

/*
 * One category on transcript page: a GE area, or one of the no-GE groups.
 * done and total count whole category, so search result that lists only some courses still shows "2 of 9".
 */
public class TranscriptGroup {

	private final String key;
	private final String title;
	private final String note;
	private final List<TranscriptCourse> courses;
	private final int done;
	private final int total;

	public TranscriptGroup(String key, String title, String note, List<TranscriptCourse> courses) {
		this(key, title, note, courses, countPassed(courses), courses.size());
	}

	private TranscriptGroup(String key, String title, String note, List<TranscriptCourse> courses, int done,
			int total) {
		this.key = key;
		this.title = title;
		this.note = note;
		this.courses = courses;
		this.done = done;
		this.total = total;
	}

	// same category with only some of its courses, counts stay those of whole category
	public TranscriptGroup withCourses(List<TranscriptCourse> some) {
		return new TranscriptGroup(key, title, note, some, done, total);
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

	public boolean isFinished() {
		return done == total;
	}
}
