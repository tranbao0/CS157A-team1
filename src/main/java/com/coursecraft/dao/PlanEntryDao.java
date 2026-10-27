package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.PlanEntry;
import com.coursecraft.model.PlannedCourse;

// SQL for plan_entries table lives here, servlets just call these methods
// every method takes caller's connection, so plan add runs all of them in one transaction
public class PlanEntryDao {

	// finds session where student already planned this course, null when course is not in plan yet
	public Integer findPlannedSessionId(Connection connection, int studentId, int courseId) throws SQLException {
		String sql = "SELECT session_id FROM plan_entries WHERE student_id = ? AND course_id = ? LIMIT 1";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setInt(2, courseId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getInt("session_id");
			}
		}
	}

	// adds up units student planned in one term, all sessions of that term count, 0 when none
	public double sumUnitsInTerm(Connection connection, int studentId, int termId) throws SQLException {
		String sql = "SELECT COALESCE(SUM(c.units), 0) AS units "
				+ "FROM plan_entries pe "
				+ "JOIN courses c ON c.course_id = pe.course_id "
				+ "JOIN sessions s ON s.session_id = pe.session_id "
				+ "WHERE pe.student_id = ? AND s.term_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setInt(2, termId);

			try (ResultSet rs = statement.executeQuery()) {
				rs.next();
				return rs.getDouble("units");
			}
		}
	}

	/*
	 * Lists every course in student's plan with its term and risk level, oldest term first.
	 * Also marks each course whose prerequisites run through petition term,
	 * so plan card can say what breaks if petition is denied.
	 *
	 * planned holds student's plan rows with term order (year * 10 + season place, see PrerequisiteDao).
	 * petition_terms finds Fall or Spring terms where planned units go over normal limit (19).
	 * open_groups keeps prerequisite groups for student's catalog year that completed courses
	 *   and student's placement do not already meet.
	 * affected starts with every course in petition term, then keeps adding later planned courses
	 *   that need affected course through open group. WITH RECURSIVE repeats that step
	 *   until no new course joins, so chains like CS 46A -> CS 46B -> CS 47 are found.
	 * dependents gives one row per affected later course, with petition term(s) and courses it goes through.
	 */
	public List<PlannedCourse> findForStudent(Connection connection, int studentId, int catalogYear,
			double normalLimit) throws SQLException {
		String sql = "WITH RECURSIVE "
				+ "planned AS ( "
				+ "  SELECT pe.plan_entry_id, pe.course_id, t.term_id, t.season, CONCAT(t.season, ' ', t.year) AS term_label, "
				+ "         t.year * 10 + FIELD(t.season, 'Winter', 'Spring', 'Summer', 'Fall') AS term_order "
				+ "  FROM plan_entries pe "
				+ "  JOIN sessions s ON s.session_id = pe.session_id "
				+ "  JOIN terms t ON t.term_id = s.term_id "
				+ "  WHERE pe.student_id = ? "
				+ "), "
				+ "petition_terms AS ( "
				+ "  SELECT p.term_id "
				+ "  FROM planned p JOIN courses c ON c.course_id = p.course_id "
				+ "  WHERE p.season IN ('Fall', 'Spring') "
				+ "  GROUP BY p.term_id "
				+ "  HAVING SUM(c.units) > ? "
				+ "), "
				+ "open_groups AS ( "
				+ "  SELECT pg.group_id, pg.course_id "
				+ "  FROM prerequisite_groups pg "
				+ "  WHERE pg.catalog_year = (SELECT MAX(pg2.catalog_year) FROM prerequisite_groups pg2 "
				+ "                           WHERE pg2.course_id = pg.course_id AND pg2.catalog_year <= ?) "
				+ "    AND NOT EXISTS (SELECT 1 FROM counts_towards ct "
				+ "                    JOIN completed cm ON cm.course_id = ct.course_id "
				+ "                    WHERE ct.group_id = pg.group_id AND cm.student_id = ? "
				+ "                      AND cm.taken_and_passed = 1) "
				+ "    AND NOT EXISTS (SELECT 1 FROM group_accepts_category ga "
				+ "                    JOIN student_placements sp ON sp.category_code = ga.category_code "
				+ "                    JOIN students st ON st.student_id = sp.student_id "
				+ "                    WHERE ga.group_id = pg.group_id AND sp.student_id = ? "
				+ "                      AND (ga.min_ppa_score IS NULL OR st.ppa_score >= ga.min_ppa_score)) "
				+ "), "
				// CAST gives first row's empty column real number type, MySQL sizes column from first row
				+ "affected (course_id, term_order, petition_term_label, through_course_id) AS ( "
				+ "  SELECT p.course_id, p.term_order, p.term_label, CAST(NULL AS SIGNED) "
				+ "  FROM planned p "
				+ "  WHERE p.term_id IN (SELECT term_id FROM petition_terms) "
				+ "  UNION "
				+ "  SELECT later.course_id, later.term_order, a.petition_term_label, a.course_id "
				+ "  FROM affected a "
				+ "  JOIN counts_towards ct ON ct.course_id = a.course_id "
				+ "  JOIN open_groups og ON og.group_id = ct.group_id "
				+ "  JOIN planned later ON later.course_id = og.course_id AND later.term_order > a.term_order "
				+ "), "
				+ "dependents AS ( "
				+ "  SELECT a.course_id, "
				+ "         GROUP_CONCAT(DISTINCT a.petition_term_label SEPARATOR ' and ') AS petition_term_label, "
				+ "         GROUP_CONCAT(DISTINCT CONCAT(tc.subject, ' ', tc.course_number) "
				+ "                      ORDER BY tc.subject, tc.course_number SEPARATOR ', ') AS through_courses "
				+ "  FROM affected a "
				+ "  JOIN courses tc ON tc.course_id = a.through_course_id "
				+ "  GROUP BY a.course_id "
				+ ") "
				+ "SELECT p.plan_entry_id, p.course_id, CONCAT(c.subject, ' ', c.course_number) AS code, c.title, c.units, "
				+ "       p.term_id, p.term_label, p.season, "
				+ "       (SELECT b.risk_level FROM bottleneck_scores b "
				+ "        WHERE b.course_id = p.course_id AND b.term_id = p.term_id "
				+ "        ORDER BY b.computed_at DESC LIMIT 1) AS risk_level, "
				+ "       d.petition_term_label, d.through_courses "
				+ "FROM planned p "
				+ "JOIN courses c ON c.course_id = p.course_id "
				+ "LEFT JOIN dependents d ON d.course_id = p.course_id "
				+ "ORDER BY p.term_order, c.subject, c.course_number";

		List<PlannedCourse> courses = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setDouble(2, normalLimit);
			statement.setInt(3, catalogYear);
			statement.setInt(4, studentId);
			statement.setInt(5, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					courses.add(new PlannedCourse(
							rs.getInt("plan_entry_id"),
							rs.getInt("course_id"),
							rs.getString("code"),
							rs.getString("title"),
							rs.getDouble("units"),
							rs.getInt("term_id"),
							rs.getString("term_label"),
							rs.getString("season"),
							rs.getString("risk_level"),
							rs.getString("petition_term_label"),
							rs.getString("through_courses")));
				}
			}
		}
		return courses;
	}

	// finds one plan row only when it belongs to this student, so nobody can touch other student's plan
	public PlanEntry findEntry(Connection connection, int studentId, int planEntryId) throws SQLException {
		String sql = "SELECT plan_entry_id, course_id, session_id FROM plan_entries "
				+ "WHERE plan_entry_id = ? AND student_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, planEntryId);
			statement.setInt(2, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new PlanEntry(rs.getInt("plan_entry_id"), rs.getInt("course_id"), rs.getInt("session_id"));
			}
		}
	}

	/*
	 * Lists later planned courses that would lose prerequisite if removed course left plan.
	 * Later course counts only when removed course sits in earlier term (so it really meets group)
	 * and nothing else meets that same group: no completed course, no placement,
	 * and no other course planned before later course's term.
	 * Term order uses year * 10 + season place, same as PrerequisiteDao.
	 */
	public List<String> findDependents(Connection connection, int studentId, int removedCourseId, int catalogYear)
			throws SQLException {
		String termOrder = "%s.year * 10 + FIELD(%s.season, 'Winter', 'Spring', 'Summer', 'Fall')";
		String sql = "SELECT DISTINCT CONCAT(c.subject, ' ', c.course_number) AS code "
				+ "FROM plan_entries later "
				+ "JOIN sessions ls ON ls.session_id = later.session_id "
				+ "JOIN terms lt ON lt.term_id = ls.term_id "
				+ "JOIN courses c ON c.course_id = later.course_id "
				+ "JOIN prerequisite_groups pg ON pg.course_id = later.course_id "
				+ "  AND pg.catalog_year = (SELECT MAX(pg2.catalog_year) FROM prerequisite_groups pg2 "
				+ "                         WHERE pg2.course_id = pg.course_id AND pg2.catalog_year <= ?) "
				+ "JOIN counts_towards removed ON removed.group_id = pg.group_id AND removed.course_id = ? "
				+ "WHERE later.student_id = ? "
				// removed course must be planned in term before later course, otherwise it never counted
				+ "  AND EXISTS (SELECT 1 FROM plan_entries rp "
				+ "              JOIN sessions rs ON rs.session_id = rp.session_id "
				+ "              JOIN terms rt ON rt.term_id = rs.term_id "
				+ "              WHERE rp.student_id = later.student_id AND rp.course_id = removed.course_id "
				+ "                AND " + String.format(termOrder, "rt", "rt") + " < " + String.format(termOrder, "lt", "lt") + ") "
				+ "  AND NOT EXISTS (SELECT 1 FROM counts_towards ct "
				+ "                  JOIN completed cm ON cm.course_id = ct.course_id "
				+ "                  WHERE ct.group_id = pg.group_id AND cm.student_id = later.student_id "
				+ "                    AND cm.taken_and_passed = 1) "
				+ "  AND NOT EXISTS (SELECT 1 FROM group_accepts_category ga "
				+ "                  JOIN student_placements sp ON sp.category_code = ga.category_code "
				+ "                  JOIN students st ON st.student_id = sp.student_id "
				+ "                  WHERE ga.group_id = pg.group_id AND sp.student_id = later.student_id "
				+ "                    AND (ga.min_ppa_score IS NULL OR st.ppa_score >= ga.min_ppa_score)) "
				+ "  AND NOT EXISTS (SELECT 1 FROM counts_towards ct "
				+ "                  JOIN plan_entries op ON op.course_id = ct.course_id "
				+ "                  JOIN sessions os ON os.session_id = op.session_id "
				+ "                  JOIN terms ot ON ot.term_id = os.term_id "
				+ "                  WHERE ct.group_id = pg.group_id AND op.student_id = later.student_id "
				+ "                    AND op.course_id <> removed.course_id "
				+ "                    AND " + String.format(termOrder, "ot", "ot") + " < " + String.format(termOrder, "lt", "lt") + ") "
				+ "ORDER BY code";

		List<String> codes = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, catalogYear);
			statement.setInt(2, removedCourseId);
			statement.setInt(3, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					codes.add(rs.getString("code"));
				}
			}
		}
		return codes;
	}

	// deletes one plan row, student_id in WHERE keeps delete inside student's own plan
	public void delete(Connection connection, int studentId, int planEntryId) throws SQLException {
		String sql = "DELETE FROM plan_entries WHERE plan_entry_id = ? AND student_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, planEntryId);
			statement.setInt(2, studentId);
			statement.executeUpdate();
		}
	}

	// adds one course to student's plan for one session, MySQL picks plan_entry_id
	public void add(Connection connection, int studentId, int courseId, int sessionId) throws SQLException {
		String sql = "INSERT INTO plan_entries (student_id, course_id, session_id, added_at) "
				+ "VALUES (?, ?, ?, NOW())";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			statement.setInt(2, courseId);
			statement.setInt(3, sessionId);
			statement.executeUpdate();
		}
	}
}
