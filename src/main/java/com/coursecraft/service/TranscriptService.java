package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.coursecraft.dao.CompletedDao;
import com.coursecraft.dao.CourseDao;
import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.GeArea;
import com.coursecraft.model.Major;
import com.coursecraft.model.TranscriptCourse;
import com.coursecraft.model.TranscriptGroup;
import com.coursecraft.util.Db;

// transcript rules live here, TranscriptServlet only reads form and picks page
public class TranscriptService {

	public List<TranscriptCourse> listCourses(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return new CourseDao().findForTranscript(connection, studentId);
		}
	}

	// majors import can match transcript's plan line to
	public List<Major> listMajors() throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return new RoadmapDao().findMajorsWithRoadmap(connection);
		}
	}

	// student's current major, null until student picks one
	public Major findMajor(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return new StudentDao().findMajor(connection, studentId);
		}
	}

	/*
	 * Sorts courses into categories, in order: each GE area that has courses, then courses with no GE area
	 * split into prerequisites, courses with prerequisites, and the rest. Empty categories are left out.
	 */
	public List<TranscriptGroup> group(List<TranscriptCourse> courses) throws SQLException {
		List<GeArea> areas;
		try (Connection connection = Db.getConnection()) {
			areas = new CourseDao().findGeAreas(connection);
		}

		List<TranscriptGroup> groups = new ArrayList<>();
		for (GeArea area : areas) {
			List<TranscriptCourse> inArea = new ArrayList<>();
			for (TranscriptCourse course : courses) {
				if (area.getCode().equals(course.getGeAreaCode())) {
					inArea.add(course);
				}
			}
			if (!inArea.isEmpty()) {
				groups.add(new TranscriptGroup("ge-" + area.getCode(),
						"Area " + area.getCode() + " - " + area.getName(), "General education", inArea));
			}
		}

		List<TranscriptCourse> prerequisites = new ArrayList<>();
		List<TranscriptCourse> withPrerequisites = new ArrayList<>();
		List<TranscriptCourse> others = new ArrayList<>();
		for (TranscriptCourse course : courses) {
			if (course.getGeAreaCode() != null) {
				continue;
			}
			if (course.isPrerequisite()) {
				prerequisites.add(course);
			} else if (course.isHasPrerequisites()) {
				withPrerequisites.add(course);
			} else {
				others.add(course);
			}
		}
		if (!prerequisites.isEmpty()) {
			groups.add(new TranscriptGroup("prereq", "Prerequisite courses",
					"No GE area. Another course requires these.", prerequisites));
		}
		if (!withPrerequisites.isEmpty()) {
			groups.add(new TranscriptGroup("has-prereq", "Courses with prerequisites",
					"No GE area. Not a prerequisite for anything else.", withPrerequisites));
		}
		if (!others.isEmpty()) {
			groups.add(new TranscriptGroup("other", "Other courses",
					"No GE area, no prerequisites, and not a prerequisite.", others));
		}
		return groups;
	}

	/*
	 * Finds courses not passed yet in given categories that match query in code or title, ignoring case.
	 * areaKey limits search to one category, empty means every category given.
	 * Each result keeps its category's counts but lists only matching courses, categories with no match are left out.
	 */
	public List<TranscriptGroup> search(List<TranscriptGroup> groups, String query, String areaKey) {
		String needle = query.toLowerCase(Locale.ROOT);
		List<TranscriptGroup> results = new ArrayList<>();
		for (TranscriptGroup group : groups) {
			if (!areaKey.isEmpty() && !areaKey.equals(group.getKey())) {
				continue;
			}
			List<TranscriptCourse> hits = new ArrayList<>();
			for (TranscriptCourse course : group.getCourses()) {
				if (!course.isPassed() && matches(course, needle)) {
					hits.add(course);
				}
			}
			if (!hits.isEmpty()) {
				results.add(group.withCourses(hits));
			}
		}
		return results;
	}

	// code is checked with and without its space, so "cs146" finds CS 146
	private boolean matches(TranscriptCourse course, String needle) {
		if (needle.isEmpty()) {
			return true;
		}
		String code = course.getCode();
		String haystack = (code + " " + code.replace(" ", "") + " " + course.getTitle()).toLowerCase(Locale.ROOT);
		return haystack.contains(needle);
	}

	/*
	 * Saves checkboxes and, when import found a major, sets student to that major in one transaction,
	 * so failure never leaves courses saved without the major or the other way round.
	 * majorId is ignored when it matches current major (keeps catalog year picked on Profile) or has no roadmap.
	 * Gives new major's name when major changed, null otherwise.
	 */
	public String save(int studentId, List<Integer> shown, Set<Integer> ticked, Integer majorId)
			throws SQLException {
		try (Connection connection = Db.getConnection()) {
			// autocommit off means nothing is saved until commit() below
			connection.setAutoCommit(false);
			try {
				StudentDao students = new StudentDao();
				String newMajor = null;
				if (majorId != null) {
					Major current = students.findMajor(connection, studentId);
					Integer roadmapId = new RoadmapDao().findNewestForMajor(connection, majorId);
					if (roadmapId != null && (current == null || current.getMajorId() != majorId)) {
						students.updateRoadmap(connection, studentId, roadmapId);
						newMajor = students.findMajor(connection, studentId).getName();
					}
				}
				new CompletedDao().saveChecked(connection, studentId, shown, ticked);
				connection.commit();
				return newMajor;
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			} finally {
				// pool hands this connection to next request, so put setting back
				connection.setAutoCommit(true);
			}
		}
	}
}
