package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.coursecraft.service.UpperDivisionGeRule;

// reads what UpperDivisionGeRule needs for one student: course areas and units, passed courses, placements, core areas
public class GeRuleDao {

	public UpperDivisionGeRule load(Connection connection, int studentId) throws SQLException {
		Map<Integer, String> courseAreas = new HashMap<>();
		Map<Integer, Double> courseUnits = new HashMap<>();
		String coursesSql = "SELECT course_id, ge_area_code, units FROM courses";
		try (PreparedStatement statement = connection.prepareStatement(coursesSql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				int courseId = rs.getInt("course_id");
				courseUnits.put(courseId, rs.getDouble("units"));
				String area = rs.getString("ge_area_code");
				if (area != null) {
					courseAreas.put(courseId, area);
				}
			}
		}

		Set<Integer> passedIds = new CompletedDao().findPassedIds(connection, studentId);
		Set<String> placementAreas = new HashSet<>(new PlacementDao().findGeAreasMet(connection, studentId).keySet());

		return new UpperDivisionGeRule(courseAreas, courseUnits, passedIds, placementAreas,
				findCoreAreas(connection, studentId));
	}

	/*
	 * Lower division GE areas student's roadmap requires, so major that skips an area (SE skips 1B) does not need it.
	 * Student with no roadmap, or roadmap with no lower division areas listed, needs every lower division area.
	 */
	private Set<String> findCoreAreas(Connection connection, int studentId) throws SQLException {
		String roadmapSql = "SELECT rga.ge_area_code FROM requires_ge_area rga "
				+ "JOIN students s ON s.roadmap_id = rga.roadmap_id "
				+ "WHERE s.student_id = ? AND rga.ge_area_code NOT LIKE 'UD%'";
		Set<String> areas = new HashSet<>();
		try (PreparedStatement statement = connection.prepareStatement(roadmapSql)) {
			statement.setInt(1, studentId);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					areas.add(rs.getString("ge_area_code"));
				}
			}
		}
		if (!areas.isEmpty()) {
			return areas;
		}

		String allSql = "SELECT code FROM ge_areas WHERE code NOT LIKE 'UD%'";
		try (PreparedStatement statement = connection.prepareStatement(allSql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				areas.add(rs.getString("code"));
			}
		}
		return areas;
	}
}
