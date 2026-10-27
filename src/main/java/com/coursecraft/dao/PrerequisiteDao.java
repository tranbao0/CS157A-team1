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
	 * Course needs EVERY group, ANY one option inside group is enough (team rule 2).
	 * Group is met by completed course (taken_and_passed = 1),
	 * by course planned in term that comes BEFORE target session's term,
	 * or by placement category student holds, with high enough PPA score when group asks for one.
	 * Same term does not count, since prerequisite must be done first.
	 *
	 * Groups come from newest catalog year that is not after student's catalog year.
	 * Each string lists choices like "MATH 19 or placement M-I (PPA 70+), M-II (PPA 70+)".
	 * Empty list means all met. CONCAT_WS skips NULL part, so group without courses lists placement only.
	 *
	 * FIELD() gives season's place in year (Winter 1, Spring 2, Summer 3, Fall 4),
	 * so year * 10 + that place puts terms in time order: Fall 2026 = 20264, Winter 2027 = 20271.
	 */
	public List<String> findMissing(Connection connection, int studentId, int courseId, int sessionId,
			int catalogYear) throws SQLException {
		String sql = "SELECT CONCAT_WS(' or ', "
				+ "         (SELECT GROUP_CONCAT(CONCAT(c.subject, ' ', c.course_number) "
				+ "                              ORDER BY c.subject, c.course_number SEPARATOR ' or ') "
				+ "          FROM counts_towards ct JOIN courses c ON c.course_id = ct.course_id "
				+ "          WHERE ct.group_id = pg.group_id), "
				+ "         CONCAT('placement ', "
				+ "                (SELECT GROUP_CONCAT(CONCAT(ga.category_code, "
				+ "                                            IF(ga.min_ppa_score IS NULL, '', "
				+ "                                               CONCAT(' (PPA ', ga.min_ppa_score, '+)'))) "
				+ "                                     ORDER BY ga.category_code SEPARATOR ', ') "
				+ "                 FROM group_accepts_category ga WHERE ga.group_id = pg.group_id))) AS choices "
				+ "FROM prerequisite_groups pg "
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
				+ "  AND NOT EXISTS ( "
				+ "    SELECT 1 FROM group_accepts_category ga "
				+ "    JOIN student_placements sp ON sp.category_code = ga.category_code "
				+ "    JOIN students st ON st.student_id = sp.student_id "
				+ "    WHERE ga.group_id = pg.group_id AND sp.student_id = ? "
				+ "      AND (ga.min_ppa_score IS NULL OR st.ppa_score >= ga.min_ppa_score)) "
				+ "ORDER BY pg.group_id";

		List<String> missing = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, courseId);
			statement.setInt(2, catalogYear);
			statement.setInt(3, studentId);
			statement.setInt(4, studentId);
			statement.setInt(5, sessionId);
			statement.setInt(6, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					missing.add(rs.getString("choices"));
				}
			}
		}
		return missing;
	}
}
