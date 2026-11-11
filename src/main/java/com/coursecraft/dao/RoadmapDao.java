package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.Major;
import com.coursecraft.model.Roadmap;

// SQL for roadmaps and majors tables lives here
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

	// roadmap with newest catalog year for one major, null when major has none
	public Integer findNewestForMajor(Connection connection, int majorId) throws SQLException {
		String sql = "SELECT roadmap_id FROM roadmaps WHERE major_id = ? ORDER BY catalog_year DESC LIMIT 1";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, majorId);

			try (ResultSet rs = statement.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getInt("roadmap_id");
			}
		}
	}
}
