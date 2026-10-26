package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SQL for bottleneck_scores table lives here, read only, scoring itself belongs to FR 5
public class BottleneckScoreDao {

	// finds newest risk level ("Low", "Medium", "High") of course in one term, null when not scored
	public String findRiskLevel(Connection connection, int courseId, int termId) throws SQLException {
		String sql = "SELECT risk_level FROM bottleneck_scores "
				+ "WHERE course_id = ? AND term_id = ? "
				+ "ORDER BY computed_at DESC LIMIT 1";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, courseId);
			statement.setInt(2, termId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getString("risk_level");
			}
		}
	}
}
