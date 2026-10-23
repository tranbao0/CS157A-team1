package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

	/*
	 * Adds new student and returns it with ID MySQL picked (AUTO_INCREMENT).
	 * Caller hashes password first, plain password never reaches here.
	 * Major and grad term stay NULL until student sets them.
	 */
	public Student create(String email, String passwordHash) throws SQLException {
		String sql = "INSERT INTO students (email, password_hash) VALUES (?, ?)";

		// RETURN_GENERATED_KEYS asks driver to hand back new student_id
		try (Connection connection = Db.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, email);
			statement.setString(2, passwordHash);
			statement.executeUpdate();

			try (ResultSet keys = statement.getGeneratedKeys()) {
				keys.next();
				int newId = keys.getInt(1);
				return new Student(newId, email, passwordHash);
			}
		}
	}
}
