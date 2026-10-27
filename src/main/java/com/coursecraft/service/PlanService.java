package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.dao.BottleneckScoreDao;
import com.coursecraft.dao.CompletedDao;
import com.coursecraft.dao.CourseDao;
import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.PrerequisiteDao;
import com.coursecraft.dao.SectionDao;
import com.coursecraft.dao.SessionDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.Course;
import com.coursecraft.model.PlanAddResult;
import com.coursecraft.model.PlanEntry;
import com.coursecraft.model.PlanRemoveResult;
import com.coursecraft.model.PlanTerm;
import com.coursecraft.model.PlanWarning;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.SessionInfo;
import com.coursecraft.util.Db;

// plan rules live here, servlet only reads request and writes answer
public class PlanService {

	/*
	 * Runs write path from requirements doc in one transaction.
	 * Checks run in this order: course and term exist, not completed, not planned yet,
	 * prerequisites met (422), unit cap (409). Only then INSERT happens.
	 * Commit only when row was added, every other answer rolls back so nothing half-done stays.
	 */
	public PlanAddResult add(int studentId, int courseId, int sessionId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			// autocommit off means nothing is saved until commit() below
			connection.setAutoCommit(false);
			try {
				PlanAddResult result = checkAndAdd(connection, studentId, courseId, sessionId);
				if (result.getStatus() == 200) {
					connection.commit();
				} else {
					connection.rollback();
				}
				return result;
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			} finally {
				// pool hands this connection to next request, so put setting back
				connection.setAutoCommit(true);
			}
		}
	}

	private PlanAddResult checkAndAdd(Connection connection, int studentId, int courseId, int sessionId)
			throws SQLException {
		// lock comes first, so every read below sees rows other requests committed before us
		int catalogYear = yearOrNewest(new StudentDao().findCatalogYearForUpdate(connection, studentId));

		Course course = new CourseDao().find(connection, courseId);
		if (course == null) {
			return new PlanAddResult(400, "That course does not exist.");
		}

		SessionDao sessionDao = new SessionDao();
		SessionInfo session = sessionDao.find(connection, sessionId);
		if (session == null) {
			return new PlanAddResult(400, "That term does not exist.");
		}

		if (new CompletedDao().hasPassed(connection, studentId, courseId)) {
			return new PlanAddResult(409,
					course.getCode() + " is already on your completed list, so it does not need to be planned.");
		}

		// one course shows up once in plan, no matter which term it is in
		PlanEntryDao planEntryDao = new PlanEntryDao();
		Integer plannedSessionId = planEntryDao.findPlannedSessionId(connection, studentId, courseId);
		if (plannedSessionId != null) {
			SessionInfo planned = sessionDao.find(connection, plannedSessionId);
			return new PlanAddResult(409,
					course.getCode() + " is already in your plan for " + planned.getLabel() + ".");
		}

		List<String> missing = new PrerequisiteDao().findMissing(connection, studentId, courseId, sessionId,
				catalogYear);
		if (!missing.isEmpty()) {
			// String.join puts ", and " between groups: "CS 146, and MATH 42 or MATH 42X"
			String message = course.getCode() + " needs " + String.join(", and ", missing) + " first.";

			// hint fits what can meet missing groups: course options, placement options, or both
			boolean courseOption = false;
			boolean placementOption = false;
			for (String group : missing) {
				if (!group.startsWith("placement ")) {
					courseOption = true;
				}
				if (group.contains("placement ")) {
					placementOption = true;
				}
			}
			if (courseOption) {
				message = message + " Plan missing courses in an earlier term, or add them to your completed list.";
			}
			if (placementOption) {
				message = message + " If you have an SJSU placement, add it on your Profile page.";
			}
			return new PlanAddResult(422, message);
		}

		double newTotal = planEntryDao.sumUnitsInTerm(connection, studentId, session.getTermId())
				+ course.getUnits();
		if (newTotal > UnitRules.capFor(session.getSeason())) {
			return new PlanAddResult(409, UnitRules.capMessage(course.getCode(), session.getTermLabel(),
					session.getSeason(), newTotal));
		}

		planEntryDao.add(connection, studentId, courseId, sessionId);

		// reads badge info from requirements doc after insert, still inside same transaction
		String riskLevel = new BottleneckScoreDao().findRiskLevel(connection, courseId, session.getTermId());
		Integer openSeats = new SectionDao().sumOpenSeats(connection, courseId, sessionId);

		String message = course.getCode() + " added to " + session.getLabel() + ". "
				+ UnitRules.describe(session.getTermLabel(), session.getSeason(), newTotal);
		return new PlanAddResult(200, message, session.getTermLabel(), newTotal,
				UnitRules.statusFor(session.getSeason(), newTotal), riskLevel, openSeats);
	}

	/*
	 * Removes one course from student's plan in one transaction, same lock as add.
	 * Refuses (409) when later planned course needs this one, so plan always passes add rules.
	 * Commit only when row was deleted, every other answer rolls back.
	 */
	public PlanRemoveResult remove(int studentId, int planEntryId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			// autocommit off means nothing is saved until commit() below
			connection.setAutoCommit(false);
			try {
				PlanRemoveResult result = checkAndRemove(connection, studentId, planEntryId);
				if (result.getStatus() == 200) {
					connection.commit();
				} else {
					connection.rollback();
				}
				return result;
			} catch (SQLException e) {
				connection.rollback();
				throw e;
			} finally {
				// pool hands this connection to next request, so put setting back
				connection.setAutoCommit(true);
			}
		}
	}

	private PlanRemoveResult checkAndRemove(Connection connection, int studentId, int planEntryId)
			throws SQLException {
		// lock comes first, so add and remove from same student never run at same time
		int catalogYear = yearOrNewest(new StudentDao().findCatalogYearForUpdate(connection, studentId));

		// findEntry only returns row when it belongs to this student
		PlanEntryDao planEntryDao = new PlanEntryDao();
		PlanEntry entry = planEntryDao.findEntry(connection, studentId, planEntryId);
		if (entry == null) {
			return new PlanRemoveResult(404, "That course is not in your plan anymore.");
		}

		Course course = new CourseDao().find(connection, entry.getCourseId());
		SessionInfo session = new SessionDao().find(connection, entry.getSessionId());

		List<String> dependents = planEntryDao.findDependents(connection, studentId, entry.getCourseId(),
				catalogYear);
		if (!dependents.isEmpty()) {
			// one course "needs" and gets named again, several courses "need" and get "those courses"
			String list = String.join(", ", dependents);
			if (dependents.size() == 1) {
				return new PlanRemoveResult(409, list + " needs " + course.getCode() + ". Remove " + list + " first.");
			}
			return new PlanRemoveResult(409, list + " need " + course.getCode() + ". Remove those courses first.");
		}

		planEntryDao.delete(connection, studentId, planEntryId);
		return new PlanRemoveResult(200, course.getCode() + " removed from " + session.getLabel() + ".");
	}

	/*
	 * Builds plan card data: student's terms in time order, each with courses, unit total and warnings.
	 * Method only reads, so it needs no transaction.
	 */
	public List<PlanTerm> summarize(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			int catalogYear = yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
			List<PlannedCourse> courses = new PlanEntryDao().findForStudent(connection, studentId, catalogYear,
					UnitRules.NORMAL_LIMIT);

			List<PlanTerm> terms = new ArrayList<>();
			PlanTerm current = null;
			for (PlannedCourse course : courses) {
				// rows come sorted by term, so new term starts when term_id changes
				if (current == null || current.getTermId() != course.getTermId()) {
					current = new PlanTerm(course.getTermId(), course.getTermLabel(), course.getSeason());
					terms.add(current);
				}
				current.addCourse(course);
			}

			// star belongs on graduation term from Profile, not just last planned term
			Integer gradTermId = new StudentDao().findProfile(connection, studentId).getGradTermId();
			for (PlanTerm term : terms) {
				addWarnings(studentId, term, courses);
				if (gradTermId != null && gradTermId == term.getTermId()) {
					term.markGradTerm();
				}
			}
			return terms;
		}
	}

	/*
	 * Adds unit warning (petition or part-time) and petition dependency warning to one term.
	 * Key has student, term and status, so minimized warning opens again when its meaning changes.
	 */
	private void addWarnings(int studentId, PlanTerm term, List<PlannedCourse> allCourses) {
		String status = term.getStatus();
		String title = UnitRules.warningTitle(status);
		if (title != null) {
			// counts later courses anywhere in plan that need this term's petition
			int dependentCount = 0;
			for (PlannedCourse course : allCourses) {
				String needs = course.getPetitionTermLabel();
				if (needs != null && needs.contains(term.getLabel())) {
					dependentCount = dependentCount + 1;
				}
			}
			String kind = "part-time";
			if ("petition".equals(status)) {
				kind = "petition";
			}
			term.addWarning(new PlanWarning(studentId + "-" + term.getTermId() + "-" + status, kind, title,
					UnitRules.warningText(status, term.getUnits(), dependentCount)));
		}

		// collects this term's courses that depend on earlier petition term
		List<String> dependentCodes = new ArrayList<>();
		String petitionTerms = null;
		for (PlannedCourse course : term.getCourses()) {
			if (course.getPetitionTermLabel() != null) {
				dependentCodes.add(course.getCode());
				petitionTerms = course.getPetitionTermLabel();
			}
		}
		if (!dependentCodes.isEmpty()) {
			String text = String.join(", ", dependentCodes) + " need courses from your " + petitionTerms
					+ " plan, which needs a petition. If the petition is denied, you may have to move them to a later term.";
			term.addWarning(new PlanWarning(studentId + "-" + term.getTermId() + "-depends", "depends",
					"Depends on petition", text));
		}
	}

	// student with no roadmap yet gets newest catalog year, since MAX in prerequisite SQL then picks newest
	private int yearOrNewest(Integer catalogYear) {
		if (catalogYear == null) {
			return Integer.MAX_VALUE;
		}
		return catalogYear;
	}
}
