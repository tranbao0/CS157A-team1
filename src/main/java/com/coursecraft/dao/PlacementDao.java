package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.PlacementCategory;

// SQL for placement_categories and student_placements tables lives here
public class PlacementDao {

	// lists every category, Math ones first, then Writing, each in code order (M-I before M-II)
	public List<PlacementCategory> findCategories(Connection connection) throws SQLException {
		String sql = "SELECT code, kind, name FROM placement_categories ORDER BY kind, code";

		List<PlacementCategory> categories = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			while (rs.next()) {
				categories.add(new PlacementCategory(rs.getString("code"), rs.getString("kind"),
						rs.getString("name")));
			}
		}
		return categories;
	}

	/*
	 * Swaps student's placements for new ones: deletes old rows, then adds each code that is not null.
	 * Caller runs this inside transaction, so student never ends up with half old, half new placements.
	 */
	public void replaceForStudent(Connection connection, int studentId, String mathCategory, String writingCategory)
			throws SQLException {
		try (PreparedStatement delete = connection.prepareStatement(
				"DELETE FROM student_placements WHERE student_id = ?")) {
			delete.setInt(1, studentId);
			delete.executeUpdate();
		}

		String insertSql = "INSERT INTO student_placements (student_id, category_code) VALUES (?, ?)";
		try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
			if (mathCategory != null) {
				insert.setInt(1, studentId);
				insert.setString(2, mathCategory);
				insert.executeUpdate();
			}
			if (writingCategory != null) {
				insert.setInt(1, studentId);
				insert.setString(2, writingCategory);
				insert.executeUpdate();
			}
		}
	}
}
