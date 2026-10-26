package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.coursecraft.model.Course;

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
}
