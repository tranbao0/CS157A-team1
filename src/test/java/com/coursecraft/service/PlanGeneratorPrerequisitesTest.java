package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.coursecraft.dao.GeRuleDao;
import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.PrerequisiteDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;
import com.coursecraft.util.Db;

class PlanGeneratorPrerequisitesTest {

	@Test
	void everyGeneratedCourseMeetsPrerequisitesInItsTerm() throws SQLException {
		List<String> failures = new ArrayList<>();
		try (Connection connection = Db.getConnection()) {
			List<Integer> students = new ArrayList<>();
			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT student_id FROM students ORDER BY student_id");
					ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					students.add(rows.getInt("student_id"));
				}
			}
			for (int studentId : students) {
				checkProposals(connection, studentId, failures);
			}
		}
		assertEquals(List.of(), failures, String.join("\n", failures));
	}

	private void checkProposals(Connection connection, int studentId, List<String> failures) throws SQLException {
		GeneratedPlan plan = new PlanGenerator().preview(connection, studentId);
		List<PlannedCourse> planned = new PlanEntryDao().findForStudent(connection, studentId);
		int firstProposal = planned.size();
		for (ProposedTerm term : plan.getTerms()) {
			for (ProposedEntry entry : term.getEntries()) {
				planned.add(new PlannedCourse(0, entry.getCourseId(), entry.getCode(), entry.getTitle(),
						entry.getUnits(), term.getTermId(), term.getLabel(), null, term.getTermOrder(), null));
			}
		}

		int year = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
		PrerequisiteDao prerequisites = new PrerequisiteDao();
		List<PrerequisiteGroup> groups = prerequisites.findOpenGroups(connection, studentId, year);
		Map<Integer, List<String>> placements = prerequisites.findPlacementOptions(connection);
		PrerequisiteCheck check = new PrerequisiteCheck();
		UpperDivisionGeRule udRule = new GeRuleDao().load(connection, studentId);
		for (int i = firstProposal; i < planned.size(); i++) {
			PlannedCourse course = planned.get(i);
			List<String> missing = check.findMissing(course.getCourseId(), course.getTermOrder(), groups, planned,
					placements);
			missing.addAll(udRule.findMissing(course.getCourseId(), course.getTermOrder(), planned, 0));
			if (!missing.isEmpty()) {
				failures.add("Student " + studentId + ", " + course.getCode() + ", " + course.getTermLabel()
						+ ": " + String.join("; ", missing));
			}
		}
	}
}
