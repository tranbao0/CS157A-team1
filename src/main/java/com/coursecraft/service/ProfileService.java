package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.coursecraft.dao.PlacementDao;
import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.dao.TermDao;
import com.coursecraft.model.PlacementCategory;
import com.coursecraft.model.Roadmap;
import com.coursecraft.model.StudentProfile;
import com.coursecraft.model.Term;
import com.coursecraft.util.Db;

// profile rules live here, ProfileServlet only reads form and picks page
public class ProfileService {

	// lists every major and catalog year for major dropdown
	public List<Roadmap> listRoadmaps() throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return new RoadmapDao().findAll(connection);
		}
	}

	// lists terms that have not ended for grad term dropdown, plus student's saved term
	public List<Term> listTerms(Integer savedTermId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return new TermDao().findUpcoming(connection, savedTermId);
		}
	}

	// lists math and writing categories, JSP splits them into two dropdowns by kind
	public List<PlacementCategory> listCategories() throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return new PlacementDao().findCategories(connection);
		}
	}

	public StudentProfile load(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			return new StudentDao().findProfile(connection, studentId);
		}
	}

	/*
	 * Checks submitted form against what dropdowns offer, gives first problem as message or null when fine.
	 * Browser only sends listed values, so these catch hand-made requests and stale pages.
	 * savedTermId is grad term already on file, it stays allowed even after it is over.
	 */
	public String check(StudentProfile profile, Integer savedTermId) throws SQLException {
		if (profile.getRoadmapId() != null) {
			boolean found = false;
			for (Roadmap roadmap : listRoadmaps()) {
				if (roadmap.getRoadmapId() == profile.getRoadmapId()) {
					found = true;
				}
			}
			if (!found) {
				return "Pick your major from the list.";
			}
		}

		if (profile.getGradTermId() != null) {
			boolean found = false;
			for (Term term : listTerms(savedTermId)) {
				if (term.getTermId() == profile.getGradTermId()) {
					found = true;
				}
			}
			if (!found) {
				return "Pick a graduation term from the list. Terms that are already over cannot be picked.";
			}
		}

		List<PlacementCategory> categories = listCategories();
		if (profile.getMathCategory() != null && !hasCategory(categories, profile.getMathCategory(), "Math")) {
			return "Pick your math enrollment category from the list.";
		}
		if (profile.getWritingCategory() != null
				&& !hasCategory(categories, profile.getWritingCategory(), "Writing")) {
			return "Pick your writing enrollment category from the list.";
		}

		if (profile.getPpaScore() != null && (profile.getPpaScore() < 0 || profile.getPpaScore() > 100)) {
			return "PPA score must be a whole number from 0 to 100.";
		}
		return null;
	}

	// tells if code exists and belongs to right kind, so math dropdown cannot save writing category
	private boolean hasCategory(List<PlacementCategory> categories, String code, String kind) {
		for (PlacementCategory category : categories) {
			if (category.getCode().equals(code) && category.getKind().equals(kind)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Saves whole form in one transaction: students row, then placements.
	 * Commit only when every write worked, so failure never leaves new major with old placement.
	 */
	public void save(int studentId, StudentProfile profile) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			// autocommit off means nothing is saved until commit() below
			connection.setAutoCommit(false);
			try {
				new StudentDao().updateProfile(connection, studentId, profile.getRoadmapId(),
						profile.getGradTermId(), profile.getPpaScore());
				new PlacementDao().replaceForStudent(connection, studentId, profile.getMathCategory(),
						profile.getWritingCategory());
				connection.commit();
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			} finally {
				// pool hands this connection to next request, so put setting back
				connection.setAutoCommit(true);
			}
		}
	}
}
