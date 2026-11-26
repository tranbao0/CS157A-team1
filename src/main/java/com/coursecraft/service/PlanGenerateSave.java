package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;
import com.coursecraft.util.Db;

// saves the plan Generate previewed, only when the plan made again still matches what the browser showed
public class PlanGenerateSave {

	/*
	Runs the save in one transaction like plan add.
	Returns how many courses were saved, 0 when the plan changed since the preview (nothing is written).
	Commit only when rows were added, every other answer rolls back.
	*/
	public int save(int studentId, List<String> posted) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			connection.setAutoCommit(false);
			try {
				int saved = checkAndSave(connection, studentId, posted);
				if (saved > 0) {
					connection.commit();
				} else {
					connection.rollback();
				}
				return saved;
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			} finally {
				connection.setAutoCommit(true);
			}
		}
	}

	private int checkAndSave(Connection connection, int studentId, List<String> posted) throws SQLException {
		// lock comes first, so the plan below sees rows other requests committed before us
		new StudentDao().findCatalogYearForUpdate(connection, studentId);

		GeneratedPlan plan = new PlanGenerator().preview(connection, studentId);
		if (!sameEntries(posted, entryKeys(plan))) {
			return 0;
		}
		PlanEntryDao planEntryDao = new PlanEntryDao();
		int saved = 0;
		for (ProposedTerm term : plan.getTerms()) {
			for (ProposedEntry entry : term.getEntries()) {
				planEntryDao.addGenerated(connection, studentId, entry.getCourseId(), entry.getSessionId(),
						entry.isSuggested());
				saved++;
			}
		}
		return saved;
	}

	/*
	Turns posted values like "12:34:true" into keys, null when any value is not
	course id, session id and true or false.
	*/
	public static List<String> parseEntries(String[] values) {
		if (values == null || values.length == 0) {
			return null;
		}
		List<String> keys = new ArrayList<>();
		for (String value : values) {
			String[] parts = value.split(":", -1);
			if (parts.length != 3) {
				return null;
			}
			int courseId = parseId(parts[0]);
			int sessionId = parseId(parts[1]);
			if (courseId == -1 || sessionId == -1) {
				return null;
			}
			if (!parts[2].equals("true") && !parts[2].equals("false")) {
				return null;
			}
			keys.add(courseId + ":" + sessionId + ":" + parts[2]);
		}
		return keys;
	}

	private static int parseId(String text) {
		int id;
		try {
			id = Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return -1;
		}
		if (id < 1) {
			return -1;
		}
		return id;
	}

	static List<String> entryKeys(GeneratedPlan plan) {
		List<String> keys = new ArrayList<>();
		for (ProposedTerm term : plan.getTerms()) {
			for (ProposedEntry entry : term.getEntries()) {
				keys.add(entry.getCourseId() + ":" + entry.getSessionId() + ":" + entry.isSuggested());
			}
		}
		return keys;
	}

	// same entries in any order, a repeated entry or an empty list never matches
	static boolean sameEntries(List<String> posted, List<String> proposed) {
		if (posted.isEmpty() || posted.size() != proposed.size()) {
			return false;
		}
		List<String> first = new ArrayList<>(posted);
		List<String> second = new ArrayList<>(proposed);
		Collections.sort(first);
		Collections.sort(second);
		return first.equals(second);
	}
}
