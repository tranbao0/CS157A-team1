package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SQL for sections and enrollment_snapshots tables lives here
public class SectionDao {

	/*
	 * Adds up open seats of course in one session, using newest snapshot of each section.
	 * Cancelled sections do not count (team rule 5).
	 * Gives null when no active section has snapshot yet.
	 */
	public Integer sumOpenSeats(Connection connection, int courseId, int sessionId) throws SQLException {
		String sql = "SELECT SUM(es.open_seats) AS open_seats "
				+ "FROM sections s "
				+ "JOIN enrollment_snapshots es ON es.section_id = s.section_id "
				+ "WHERE s.course_id = ? AND s.session_id = ? AND s.status = 'Active' "
				+ "  AND es.captured_at = (SELECT MAX(es2.captured_at) FROM enrollment_snapshots es2 "
				+ "                        WHERE es2.section_id = s.section_id)";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, courseId);
			statement.setInt(2, sessionId);

			try (ResultSet rs = statement.executeQuery()) {
				rs.next();
				int seats = rs.getInt("open_seats");
				// getInt gives 0 for SQL NULL, wasNull tells real 0 apart from no data
				if (rs.wasNull()) {
					return null;
				}
				return seats;
			}
		}
	}
}
