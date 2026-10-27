package com.coursecraft.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.model.Term;

// SQL for terms table lives here
public class TermDao {

	/*
	 * Lists terms student can still graduate in: last session of term ends today or later.
	 * Also keeps keepTermId (student's saved term) even when it is over, so saving form never drops it.
	 * FIELD() puts Winter, Spring, Summer, Fall in time order inside one year.
	 */
	public List<Term> findUpcoming(Connection connection, Integer keepTermId) throws SQLException {
		String sql = "SELECT t.term_id, CONCAT(t.season, ' ', t.year) AS label FROM terms t "
				+ "WHERE t.term_id IN (SELECT s.term_id FROM sessions s "
				+ "                    GROUP BY s.term_id HAVING MAX(s.end_date) >= CURDATE()) "
				+ "   OR t.term_id = ? "
				+ "ORDER BY t.year, FIELD(t.season, 'Winter', 'Spring', 'Summer', 'Fall')";

		List<Term> terms = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			// no saved term means = NULL, which matches no row
			if (keepTermId == null) {
				statement.setNull(1, Types.INTEGER);
			} else {
				statement.setInt(1, keepTermId);
			}

			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					terms.add(new Term(rs.getInt("term_id"), rs.getString("label")));
				}
			}
		}
		return terms;
	}
}
