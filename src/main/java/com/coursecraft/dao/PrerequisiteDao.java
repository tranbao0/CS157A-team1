package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.PrerequisiteGroup;

// SQL for prerequisite_groups, counts_towards and group_accepts_category tables lives here
public class PrerequisiteDao {

	// lists every open group of every course, plan card and plan remove walk through these
	public List<PrerequisiteGroup> findOpenGroups(Connection connection, int studentId, int catalogYear)
			throws SQLException {
		return readOpenGroups(connection, studentId, catalogYear, null);
	}

	// lists open groups of one course only, plan add checks these before INSERT
	public List<PrerequisiteGroup> findOpenGroupsForCourse(Connection connection, int studentId, int courseId,
			int catalogYear) throws SQLException {
		return readOpenGroups(connection, studentId, catalogYear, courseId);
	}

	/*
	 * Runs open group query for both methods above, one row per group option.
	 * Open means no completed course (taken_and_passed = 1) and no placement of student meets group yet.
	 * PrerequisiteCheck then compares student's planned courses and their terms with these groups.
	 * Groups come from newest catalog year that is not after student's catalog year.
	 * LEFT JOIN keeps placement-only group too, its option columns come back NULL.
	 */
	private List<PrerequisiteGroup> readOpenGroups(Connection connection, int studentId, int catalogYear,
			Integer courseId) throws SQLException {
		String sql = "SELECT pg.group_id, pg.course_id, ct.course_id AS option_id, "
				+ "       CONCAT(c.subject, ' ', c.course_number) AS option_code "
				+ "FROM prerequisite_groups pg "
				+ "LEFT JOIN counts_towards ct ON ct.group_id = pg.group_id "
				+ "LEFT JOIN courses c ON c.course_id = ct.course_id "
				+ "WHERE pg.catalog_year = (SELECT MAX(pg2.catalog_year) FROM prerequisite_groups pg2 "
				+ "                         WHERE pg2.course_id = pg.course_id AND pg2.catalog_year <= ?) "
				+ "  AND NOT EXISTS (SELECT 1 FROM counts_towards met "
				+ "                  JOIN completed cm ON cm.course_id = met.course_id "
				+ "                  WHERE met.group_id = pg.group_id AND cm.student_id = ? "
				+ "                    AND cm.taken_and_passed = 1) "
				+ "  AND NOT EXISTS (SELECT 1 FROM group_accepts_category ga "
				+ "                  JOIN student_placements sp ON sp.category_code = ga.category_code "
				+ "                  JOIN students st ON st.student_id = sp.student_id "
				+ "                  WHERE ga.group_id = pg.group_id AND sp.student_id = ? "
				+ "                    AND (ga.min_ppa_score IS NULL OR st.ppa_score >= ga.min_ppa_score)) ";
		if (courseId != null) {
			sql = sql + "  AND pg.course_id = ? ";
		}
		sql = sql + "ORDER BY pg.group_id, c.subject, c.course_number";

		List<PrerequisiteGroup> groups = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, catalogYear);
			statement.setInt(2, studentId);
			statement.setInt(3, studentId);
			if (courseId != null) {
				statement.setInt(4, courseId);
			}

			try (ResultSet rs = statement.executeQuery()) {
				PrerequisiteGroup current = null;
				while (rs.next()) {
					// rows come sorted by group, so new group starts when group_id changes
					int groupId = rs.getInt("group_id");
					if (current == null || current.getGroupId() != groupId) {
						current = new PrerequisiteGroup(groupId, rs.getInt("course_id"));
						groups.add(current);
					}
					int optionId = rs.getInt("option_id");
					// getInt gives 0 for SQL NULL, wasNull tells placement-only group apart from real option
					if (!rs.wasNull()) {
						current.addOption(optionId, rs.getString("option_code"));
					}
				}
			}
		}
		return groups;
	}

	// lists placement options of one group like "M-I (PPA 70+)" or "W-II", plan add message shows them
	public List<String> findPlacementOptions(Connection connection, int groupId) throws SQLException {
		String sql = "SELECT category_code, min_ppa_score FROM group_accepts_category "
				+ "WHERE group_id = ? ORDER BY category_code";

		List<String> options = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, groupId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					String option = rs.getString("category_code");
					int minScore = rs.getInt("min_ppa_score");
					// NULL score means any PPA score works, so nothing gets added
					if (!rs.wasNull()) {
						option = option + " (PPA " + minScore + "+)";
					}
					options.add(option);
				}
			}
		}
		return options;
	}
}
