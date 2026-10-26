package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// SQL for prerequisite_groups and counts_towards tables lives here
public class PrerequisiteDao {

	/*
	 * Finds prerequisite groups student has not met yet for one course, one string per group.
	 * Course needs EVERY group, ANY one course inside group is enough (team rule 2).
	 * Group is met by completed course (taken_and_passed = 1)
	 * or by course planned in term that comes BEFORE target session's term.
	 * Same term does not count, since prerequisite must be done first.
	 *
	 * Groups come from newest catalog year that is not after student's catalog year.
	 * Each string lists choices like "CS 146 or CS 151". Empty list means all met.
	 *
	 * FIELD() gives season's place in year (Winter 1, Spring 2, Summer 3, Fall 4),
	 * so year * 10 + that place puts terms in time order: Fall 2026 = 20264, Winter 2027 = 20271.
	 */
	public List<String> findMissing(Connection connection, int studentId, int courseId, int sessionId,
			int catalogYear) throws SQLException {
		String sql = "SELECT GROUP_CONCAT(CONCAT(c.subject, ' ', c.course_number) "
				+ "                    ORDER BY c.subject, c.course_number SEPARATOR ' or ') AS choices "
				+ "FROM prerequisite_groups pg "
				+ "JOIN counts_towards ct ON ct.group_id = pg.group_id "
				+ "JOIN courses c ON c.course_id = ct.course_id "
				+ "WHERE pg.course_id = ? "
				+ "  AND pg.catalog_year = (SELECT MAX(pg2.catalog_year) FROM prerequisite_groups pg2 "
				+ "                         WHERE pg2.course_id = pg.course_id AND pg2.catalog_year <= ?) "
				+ "  AND NOT EXISTS ( "
				+ "    SELECT 1 FROM counts_towards met "
				+ "    WHERE met.group_id = pg.group_id "
				+ "      AND (met.course_id IN (SELECT cm.course_id FROM completed cm "
				+ "                             WHERE cm.student_id = ? AND cm.taken_and_passed = 1) "
				+ "        OR met.course_id IN (SELECT pe.course_id FROM plan_entries pe "
				+ "                             JOIN sessions ps ON ps.session_id = pe.session_id "
				+ "                             JOIN terms pt ON pt.term_id = ps.term_id "
				+ "                             WHERE pe.student_id = ? "
				+ "                               AND pt.year * 10 + FIELD(pt.season, 'Winter', 'Spring', 'Summer', 'Fall') "
				+ "                                 < (SELECT tt.year * 10 + FIELD(tt.season, 'Winter', 'Spring', 'Summer', 'Fall') "
				+ "                                    FROM sessions ts JOIN terms tt ON tt.term_id = ts.term_id "
				+ "                                    WHERE ts.session_id = ?)))) "
				+ "GROUP BY pg.group_id "
				+ "ORDER BY pg.group_id";

		List<String> missing = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, courseId);
			statement.setInt(2, catalogYear);
			statement.setInt(3, studentId);
			statement.setInt(4, studentId);
			statement.setInt(5, sessionId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					missing.add(rs.getString("choices"));
				}
			}
		}
		return missing;
	}
}
