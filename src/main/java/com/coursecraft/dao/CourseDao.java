package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.Course;
import com.coursecraft.model.GeArea;
import com.coursecraft.model.TranscriptCourse;

// SQL for courses table lives here, servlets just call these methods
public class CourseDao {

	// finds course with short name like "CS 146", null when no course has that ID
	// caller passes connection so this read joins caller's transaction
	public Course find(Connection connection, int courseId) throws SQLException {
		String sql = "SELECT course_id, CONCAT(subject, ' ', course_number) AS code, units "
				+ "FROM courses WHERE course_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, courseId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new Course(rs.getInt("course_id"), rs.getString("code"), rs.getDouble("units"));
			}
		}
	}

	/*
	 * Every course for transcript page, with whether it is a prerequisite, has prerequisites, and is passed.
	 * Prerequisite flags use rules for student's catalog year, newest rules when student has no roadmap yet.
	 * passed subquery gives NULL when student has no completed row, getBoolean reads that as false.
	 * Numeric part first, so 46A comes before 100W.
	 */
	public List<TranscriptCourse> findForTranscript(Connection connection, int studentId) throws SQLException {
		int catalogYear = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));

		String sql = "SELECT c.course_id, c.subject, c.course_number, c.title, c.units, c.ge_area_code, "
				+ "       EXISTS (SELECT 1 FROM counts_towards ct "
				+ "               JOIN prerequisite_groups pg ON pg.group_id = ct.group_id "
				+ "               WHERE ct.course_id = c.course_id "
				+ "               AND pg.catalog_year = (SELECT MAX(pg2.catalog_year) FROM prerequisite_groups pg2 "
				+ "                                      WHERE pg2.course_id = pg.course_id AND pg2.catalog_year <= ?)) AS is_prereq, "
				+ "       EXISTS (SELECT 1 FROM prerequisite_groups pg WHERE pg.course_id = c.course_id AND pg.catalog_year <= ?) AS has_prereq, "
				+ "       (SELECT cp.taken_and_passed FROM completed cp "
				+ "        WHERE cp.student_id = ? AND cp.course_id = c.course_id) AS passed "
				+ "FROM courses c "
				+ "ORDER BY c.subject, CAST(c.course_number AS UNSIGNED), c.course_number";

		List<TranscriptCourse> courses = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, catalogYear);
			statement.setInt(2, catalogYear);
			statement.setInt(3, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					courses.add(new TranscriptCourse(rs.getInt("course_id"), rs.getString("subject"),
							rs.getString("course_number"), rs.getString("title"), rs.getString("units"),
							rs.getString("ge_area_code"), rs.getBoolean("is_prereq"), rs.getBoolean("has_prereq"),
							rs.getBoolean("passed")));
				}
			}
		}
		return courses;
	}

	// lists every GE area, A1 first
	public List<GeArea> findGeAreas(Connection connection) throws SQLException {
		String sql = "SELECT code, name FROM ge_areas ORDER BY code";

		List<GeArea> areas = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				areas.add(new GeArea(rs.getString("code"), rs.getString("name")));
			}
		}
		return areas;
	}
}
