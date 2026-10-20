package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.coursecraft.model.Student;
import com.coursecraft.util.Db;

// SQL for students table lives here, servlets just call these methods
public class StudentDao {

	// null when no student has that email
	public Student findByEmail(String email) throws SQLException {
		String sql = "SELECT student_id, email, password_hash FROM students WHERE email = ?";

		try (Connection connection = Db.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, email);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new Student(
						rs.getInt("student_id"),
						rs.getString("email"),
						rs.getString("password_hash"));
			}
		}
	}
}
