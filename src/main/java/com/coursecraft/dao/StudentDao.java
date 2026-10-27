package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import com.coursecraft.model.Student;
import com.coursecraft.model.StudentProfile;
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
	 * Reads profile form values for one student, null when student does not exist.
	 * Each subquery picks student's one placement of that kind, NULL when none is on file.
	 */
	public StudentProfile findProfile(Connection connection, int studentId) throws SQLException {
		String sql = "SELECT s.roadmap_id, s.expected_grad_term_id, s.ppa_score, "
				+ "       (SELECT sp.category_code FROM student_placements sp "
				+ "        JOIN placement_categories pc ON pc.code = sp.category_code "
				+ "        WHERE sp.student_id = s.student_id AND pc.kind = 'Math' LIMIT 1) AS math_category, "
				+ "       (SELECT sp.category_code FROM student_placements sp "
				+ "        JOIN placement_categories pc ON pc.code = sp.category_code "
				+ "        WHERE sp.student_id = s.student_id AND pc.kind = 'Writing' LIMIT 1) AS writing_category "
				+ "FROM students s WHERE s.student_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new StudentProfile(
						readInteger(rs, "roadmap_id"),
						readInteger(rs, "expected_grad_term_id"),
						readInteger(rs, "ppa_score"),
						rs.getString("math_category"),
						rs.getString("writing_category"));
			}
		}
	}

	// saves major, grad term and PPA score, null values store SQL NULL
	public void updateProfile(Connection connection, int studentId, Integer roadmapId, Integer gradTermId,
			Integer ppaScore) throws SQLException {
		String sql = "UPDATE students SET roadmap_id = ?, expected_grad_term_id = ?, ppa_score = ? "
				+ "WHERE student_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			setInteger(statement, 1, roadmapId);
			setInteger(statement, 2, gradTermId);
			setInteger(statement, 3, ppaScore);
			statement.setInt(4, studentId);
			statement.executeUpdate();
		}
	}

	// getInt gives 0 for SQL NULL, wasNull tells empty column apart from real 0
	private Integer readInteger(ResultSet rs, String column) throws SQLException {
		int value = rs.getInt(column);
		if (rs.wasNull()) {
			return null;
		}
		return value;
	}

	// setInt cannot take null, so empty value goes in with setNull
	private void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
		if (value == null) {
			statement.setNull(index, Types.INTEGER);
		} else {
			statement.setInt(index, value);
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
