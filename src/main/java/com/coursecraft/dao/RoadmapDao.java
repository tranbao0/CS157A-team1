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

// SQL for roadmaps, majors, requires_course, and requires_ge_area tables lives here
public class RoadmapDao {

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