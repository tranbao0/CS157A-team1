package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.coursecraft.model.SessionInfo;

// SQL for sessions table lives here, servlets just call these methods
public class SessionDao {

	/*
	 * Finds session with its term, null when no session has that ID.
	 * Regular session gets label "Fall 2026",
	 * others add their name like "Summer 2026 (Summer Session 1)".
	 * Term label skips session name, so it is "Summer 2026" for both summer sessions.
	 */
	public SessionInfo find(Connection connection, int sessionId) throws SQLException {
		String sql = "SELECT se.session_id, se.term_id, t.season, "
				+ "       CONCAT(t.season, ' ', t.year) AS term_label, "
				+ "       CASE WHEN se.name = 'Regular' "
				+ "            THEN CONCAT(t.season, ' ', t.year) "
				+ "            ELSE CONCAT(t.season, ' ', t.year, ' (', se.name, ')') "
				+ "       END AS label "
				+ "FROM sessions se JOIN terms t ON t.term_id = se.term_id "
				+ "WHERE se.session_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, sessionId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new SessionInfo(rs.getInt("session_id"), rs.getInt("term_id"),
						rs.getString("season"), rs.getString("label"), rs.getString("term_label"));
			}
		}
	}
}
