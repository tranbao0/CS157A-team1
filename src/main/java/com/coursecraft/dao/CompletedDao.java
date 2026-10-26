package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SQL for completed table lives here, servlets just call these methods
public class CompletedDao {

	// tells if student passed course or is taking it now (taken_and_passed = 1)
	public boolean hasPassed(Connection connection, int studentId, int courseId) throws SQLException {
		String sql = "SELECT 1 FROM completed "
				+ "WHERE student_id = ? AND course_id = ? AND taken_and_passed = 1";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setInt(2, courseId);

			try (ResultSet rs = statement.executeQuery()) {
				return rs.next();
			}
		}
	}
}
