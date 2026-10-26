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

	// gives catalog year of student's roadmap, null when student has no roadmap yet
	public Integer findCatalogYear(Connection connection, int studentId) throws SQLException {
		return readCatalogYear(connection, studentId, false);
	}

	/*
	 * Same as findCatalogYear, but also locks student's row until caller's transaction ends.
	 * Second plan add from same student (other tab, double request) waits here until first one commits,
	 * so both cannot pass unit cap check at same time. FOR UPDATE OF s locks only students row.
	 */
	public Integer findCatalogYearForUpdate(Connection connection, int studentId) throws SQLException {
		return readCatalogYear(connection, studentId, true);
	}

	// runs catalog year query for both methods above, lock adds FOR UPDATE OF s at end
	private Integer readCatalogYear(Connection connection, int studentId, boolean lock) throws SQLException {
		String sql = "SELECT r.catalog_year FROM students s "
				+ "LEFT JOIN roadmaps r ON r.roadmap_id = s.roadmap_id "
				+ "WHERE s.student_id = ?";
		if (lock) {
			sql = sql + " FOR UPDATE OF s";
		}

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				int year = rs.getInt("catalog_year");
				// getInt gives 0 for SQL NULL, wasNull tells no roadmap apart from real year
				if (rs.wasNull()) {
					return null;
				}
				return year;
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
