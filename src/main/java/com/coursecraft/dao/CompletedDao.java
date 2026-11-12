package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

// SQL for completed table lives here, servlets just call these methods
public class CompletedDao {

	/*
	 * Saves transcript checkboxes: ticked course becomes passed row, unticked course loses its passed row.
	 * Only courses in shown are touched, so courses student never saw on page keep what they had.
	 * Delete keeps not-passed rows (taken_and_passed = 0), since search shows those courses unticked
	 * and saving with the search open would otherwise wipe a failed attempt student never touched.
	 * INSERT ... SELECT FROM courses writes nothing for an ID with no course, so made-up IDs are skipped.
	 * Caller passes connection and owns transaction, so every row saves or none does.
	 */
	public void saveChecked(Connection connection, int studentId, List<Integer> shown, Set<Integer> ticked)
			throws SQLException {
		String keepSql = "INSERT INTO completed (student_id, course_id, taken_and_passed) "
				+ "SELECT ?, course_id, 1 FROM courses WHERE course_id = ? "
				+ "ON DUPLICATE KEY UPDATE taken_and_passed = 1";
		String dropSql = "DELETE FROM completed WHERE student_id = ? AND course_id = ? AND taken_and_passed = 1";

		try (PreparedStatement keep = connection.prepareStatement(keepSql);
				PreparedStatement drop = connection.prepareStatement(dropSql)) {
			for (int courseId : shown) {
				PreparedStatement statement = ticked.contains(courseId) ? keep : drop;
				statement.setInt(1, studentId);
				statement.setInt(2, courseId);
				statement.addBatch();
			}
			// runs queued rows, driver still sends one statement per row since Db URL has no rewriteBatchedStatements
			keep.executeBatch();
			drop.executeBatch();
		}
	}

	// Reads all passed course IDs once for the plan actions on a page.
	public Set<Integer> findPassedIds(Connection connection, int studentId) throws SQLException {
		Set<Integer> ids = new HashSet<>();
		String sql = "SELECT course_id FROM completed WHERE student_id = ? AND taken_and_passed = 1";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					ids.add(rs.getInt("course_id"));
				}
			}
		}
		return ids;
	}

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
