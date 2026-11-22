package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.Major;
import com.coursecraft.model.Roadmap;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.RoadmapGeStatus;

// SQL for roadmaps, majors, requires_course, requires_ge_area, and elective tables lives here
public class RoadmapDao {

	public static class ElectiveGroupRow {
		public final int groupId;
		public final String name;
		public final Double minUnits;
		public final Integer minCourses;

		public ElectiveGroupRow(int groupId, String name, Double minUnits, Integer minCourses) {
			this.groupId = groupId;
			this.name = name;
			this.minUnits = minUnits;
			this.minCourses = minCourses;
		}
	}

	public static class ElectiveSlotRow {
		public final int slotId;
		public final int groupId;
		public final String groupName;
		public final int suggestedTerm;
		public final double units;

		public ElectiveSlotRow(int slotId, int groupId, String groupName, int suggestedTerm, double units) {
			this.slotId = slotId;
			this.groupId = groupId;
			this.groupName = groupName;
			this.suggestedTerm = suggestedTerm;
			this.units = units;
		}
	}

	public static class GroupTakenCourse {
		public final int groupId;
		public final RoadmapCourse course;

		public GroupTakenCourse(int groupId, RoadmapCourse course) {
			this.groupId = groupId;
			this.course = course;
		}
	}

	// lists every roadmap with its major name, majors A to Z and newest catalog year first
	public List<Roadmap> findAll(Connection connection) throws SQLException {
		String sql = "SELECT r.roadmap_id, m.name AS major_name, r.catalog_year "
				+ "FROM roadmaps r JOIN majors m ON m.major_id = r.major_id "
				+ "ORDER BY m.name, r.catalog_year DESC";

		List<Roadmap> roadmaps = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				roadmaps.add(new Roadmap(rs.getInt("roadmap_id"), rs.getString("major_name"),
						rs.getInt("catalog_year")));
			}
		}
		return roadmaps;
	}

	// majors that have at least one roadmap, since a major without one cannot be saved on student
	public List<Major> findMajorsWithRoadmap(Connection connection) throws SQLException {
		String sql = "SELECT DISTINCT m.major_id, m.name FROM majors m JOIN roadmaps r ON r.major_id = m.major_id";

		List<Major> majors = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				majors.add(new Major(rs.getInt("major_id"), rs.getString("name")));
			}
		}
		return majors;
	}

	/*
	 * Roadmap of one major for a catalog year, null when major has none.
	 * Picks that year when major has it, else newest year before it, else closest year after it.
	 * catalogYear null (student had no major yet) picks newest year.
	 * First ORDER BY part puts years at or before catalogYear first, second sorts them newest first
	 * and later years oldest first.
	 */
	public Integer findForMajor(Connection connection, int majorId, Integer catalogYear) throws SQLException {
		String sql = "SELECT roadmap_id FROM roadmaps WHERE major_id = ? "
				+ "ORDER BY catalog_year <= ? DESC, "
				+ "         CASE WHEN catalog_year <= ? THEN -catalog_year ELSE catalog_year END "
				+ "LIMIT 1";

		int year = catalogYear == null ? Integer.MAX_VALUE : catalogYear;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, majorId);
			statement.setInt(2, year);
			statement.setInt(3, year);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getInt("roadmap_id");
			}
		}
	}

	// student's active roadmap (major name + catalog year), null until student picks one on Profile
	public Roadmap findForStudent(Connection connection, int studentId) throws SQLException {
		String sql = "SELECT r.roadmap_id, m.name AS major_name, r.catalog_year "
				+ "FROM students s "
				+ "JOIN roadmaps r ON r.roadmap_id = s.roadmap_id "
				+ "JOIN majors m ON m.major_id = r.major_id "
				+ "WHERE s.student_id = ?";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new Roadmap(rs.getInt("roadmap_id"), rs.getString("major_name"),
						rs.getInt("catalog_year"));
			}
		}
	}

	// required courses for student's roadmap in suggested term order, with passed flag and earliest planned term
	public List<RoadmapCourse> findCoursesForStudent(Connection connection, int studentId) throws SQLException {
		String sql = "SELECT c.course_id, c.subject, c.course_number, c.title, c.units, "
				+ "       rc.suggested_term_position, rc.requirement_type, "
				+ "       EXISTS (SELECT 1 FROM completed cp "
				+ "               WHERE cp.student_id = s.student_id AND cp.course_id = c.course_id "
				+ "                 AND cp.taken_and_passed = 1) AS passed, "
				+ "       (SELECT CONCAT(t.season, ' ', t.year) "
				+ "        FROM plan_entries pe "
				+ "        JOIN sessions se ON se.session_id = pe.session_id "
				+ "        JOIN terms t ON t.term_id = se.term_id "
				+ "        WHERE pe.student_id = s.student_id AND pe.course_id = c.course_id "
				+ "        ORDER BY t.year, FIELD(t.season, 'Winter', 'Spring', 'Summer', 'Fall') "
				+ "        LIMIT 1) AS planned_term "
				+ "FROM students s "
				+ "JOIN requires_course rc ON rc.roadmap_id = s.roadmap_id "
				+ "JOIN courses c ON c.course_id = rc.course_id "
				+ "WHERE s.student_id = ? "
				+ "ORDER BY rc.suggested_term_position, FIELD(rc.requirement_type, 'Major', 'Support', 'Elective'), "
				+ "         c.subject, CAST(c.course_number AS UNSIGNED), c.course_number";

		List<RoadmapCourse> courses = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					courses.add(new RoadmapCourse(
							rs.getInt("course_id"),
							rs.getString("subject"),
							rs.getString("course_number"),
							rs.getString("title"),
							rs.getString("units"),
							rs.getInt("suggested_term_position"),
							rs.getString("requirement_type"),
							rs.getBoolean("passed"),
							rs.getString("planned_term")));
				}
			}
		}
		return courses;
	}

	// elective group rules for student's roadmap
	public List<ElectiveGroupRow> findElectiveGroupsForStudent(Connection connection, int studentId)
			throws SQLException {
		String sql = "SELECT eg.group_id, eg.name, eg.min_units, eg.min_courses "
				+ "FROM students s "
				+ "JOIN elective_groups eg ON eg.roadmap_id = s.roadmap_id "
				+ "WHERE s.student_id = ? "
				+ "ORDER BY eg.group_id";

		List<ElectiveGroupRow> groups = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					double u = rs.getDouble("min_units");
					Double minUnits = rs.wasNull() ? null : u;
					int c = rs.getInt("min_courses");
					Integer minCourses = rs.wasNull() ? null : c;
					groups.add(new ElectiveGroupRow(
							rs.getInt("group_id"), rs.getString("name"), minUnits, minCourses));
				}
			}
		}
		return groups;
	}

	// elective placeholder slots in suggested term order
	public List<ElectiveSlotRow> findElectiveSlotsForStudent(Connection connection, int studentId)
			throws SQLException {
		String sql = "SELECT es.slot_id, es.group_id, eg.name AS group_name, "
				+ "       es.suggested_term_position, es.units "
				+ "FROM students s "
				+ "JOIN elective_groups eg ON eg.roadmap_id = s.roadmap_id "
				+ "JOIN elective_slots es ON es.group_id = eg.group_id "
				+ "WHERE s.student_id = ? "
				+ "ORDER BY es.suggested_term_position, es.slot_id";

		List<ElectiveSlotRow> slots = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					slots.add(new ElectiveSlotRow(
							rs.getInt("slot_id"),
							rs.getInt("group_id"),
							rs.getString("group_name"),
							rs.getInt("suggested_term_position"),
							rs.getDouble("units")));
				}
			}
		}
		return slots;
	}

	// passed or planned courses per elective group: taken courses first, then planned by earliest term
	public List<GroupTakenCourse> findTakenElectivesForStudent(Connection connection, int studentId)
			throws SQLException {
		String sql = "SELECT eg.group_id, c.course_id, c.subject, c.course_number, c.title, c.units, "
				+ "       EXISTS (SELECT 1 FROM completed cp "
				+ "               WHERE cp.student_id = s.student_id AND cp.course_id = c.course_id "
				+ "                 AND cp.taken_and_passed = 1) AS passed, "
				+ "       (SELECT CONCAT(t.season, ' ', t.year) "
				+ "        FROM plan_entries pe "
				+ "        JOIN sessions se ON se.session_id = pe.session_id "
				+ "        JOIN terms t ON t.term_id = se.term_id "
				+ "        WHERE pe.student_id = s.student_id AND pe.course_id = c.course_id "
				+ "        ORDER BY t.year, FIELD(t.season, 'Winter', 'Spring', 'Summer', 'Fall') "
				+ "        LIMIT 1) AS planned_term, "
				+ "       COALESCE((SELECT t.year * 10 + FIELD(t.season, 'Winter', 'Spring', 'Summer', 'Fall') "
				+ "                 FROM plan_entries pe "
				+ "                 JOIN sessions se ON se.session_id = pe.session_id "
				+ "                 JOIN terms t ON t.term_id = se.term_id "
				+ "                 WHERE pe.student_id = s.student_id AND pe.course_id = c.course_id "
				+ "                 ORDER BY t.year, FIELD(t.season, 'Winter', 'Spring', 'Summer', 'Fall') "
				+ "                 LIMIT 1), 999999) AS term_order "
				+ "FROM students s "
				+ "JOIN elective_groups eg ON eg.roadmap_id = s.roadmap_id "
				+ "JOIN elective_group_courses egc ON egc.group_id = eg.group_id "
				+ "JOIN courses c ON c.course_id = egc.course_id "
				+ "WHERE s.student_id = ? "
				+ "  AND (EXISTS (SELECT 1 FROM completed cp "
				+ "               WHERE cp.student_id = s.student_id AND cp.course_id = c.course_id "
				+ "                 AND cp.taken_and_passed = 1) "
				+ "    OR EXISTS (SELECT 1 FROM plan_entries pe "
				+ "               WHERE pe.student_id = s.student_id AND pe.course_id = c.course_id)) "
				+ "ORDER BY eg.group_id, passed DESC, term_order, "
				+ "         c.subject, CAST(c.course_number AS UNSIGNED), c.course_number";

		List<GroupTakenCourse> list = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					RoadmapCourse rc = new RoadmapCourse(
							rs.getInt("course_id"),
							rs.getString("subject"),
							rs.getString("course_number"),
							rs.getString("title"),
							rs.getString("units"),
							0,
							"Elective",
							rs.getBoolean("passed"),
							rs.getString("planned_term"));
					list.add(new GroupTakenCourse(rs.getInt("group_id"), rc));
				}
			}
		}
		return list;
	}

	// required GE areas for student's roadmap, checking completed courses, placements (M-I -> 2, W-I -> 1A), and 5C science lab
	public List<RoadmapGeStatus> findGeAreasForStudent(Connection connection, int studentId) throws SQLException {
		String sql = "SELECT ga.code, ga.name, "
				+ "       COALESCE( "
				+ "           (SELECT CONCAT(c.subject, ' ', c.course_number) "
				+ "            FROM completed cp "
				+ "            JOIN courses c ON c.course_id = cp.course_id "
				+ "            WHERE cp.student_id = s.student_id "
				+ "              AND cp.taken_and_passed = 1 "
				+ "              AND c.ge_area_code = ga.code "
				+ "            ORDER BY c.subject, CAST(c.course_number AS UNSIGNED), c.course_number LIMIT 1), "
				+ "           (SELECT CONCAT('Placement (', sp.category_code, ')') "
				+ "            FROM student_placements sp "
				+ "            WHERE sp.student_id = s.student_id "
				+ "              AND ((ga.code = '1A' AND sp.category_code = 'W-I') "
				+ "                OR (ga.code = '2' AND sp.category_code = 'M-I')) "
				+ "            LIMIT 1), "
				+ "           (SELECT CONCAT(c.subject, ' ', c.course_number, ' (Lab)') "
				+ "            FROM completed cp "
				+ "            JOIN courses c ON c.course_id = cp.course_id "
				+ "            WHERE ga.code = '5C' "
				+ "              AND cp.student_id = s.student_id "
				+ "              AND cp.taken_and_passed = 1 "
				+ "              AND c.ge_area_code IN ('5A', '5B') "
				+ "              AND c.units >= 4.0 "
				+ "            ORDER BY c.subject, CAST(c.course_number AS UNSIGNED), c.course_number LIMIT 1) "
				+ "       ) AS fulfilled_by, "
				+ "       EXISTS (SELECT 1 FROM courses c WHERE c.ge_area_code = ga.code) AS has_courses "
				+ "FROM students s "
				+ "JOIN requires_ge_area rga ON rga.roadmap_id = s.roadmap_id "
				+ "JOIN ge_areas ga ON ga.code = rga.ge_area_code "
				+ "WHERE s.student_id = ? "
				+ "ORDER BY ga.code";

		List<RoadmapGeStatus> areas = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, studentId);

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					areas.add(new RoadmapGeStatus(
							rs.getString("code"),
							rs.getString("name"),
							rs.getString("fulfilled_by"),
							rs.getBoolean("has_courses")));
				}
			}
		}
		return areas;
	}
}