package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
}
