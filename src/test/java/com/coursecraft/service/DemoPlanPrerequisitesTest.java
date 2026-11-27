package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.PrerequisiteDao;
import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.util.Db;

/*
 * Talks to real local MySQL, needs schema.sql and seed.sql loaded first.
 * seed.sql says every row of demo CS student 13 and SE student 14 passes POST /plan/add rules,
 * so each planned course must have no missing prerequisite, UD GE rule included.
 */
class DemoPlanPrerequisitesTest {

	@ParameterizedTest
	@ValueSource(ints = { 13, 14 })
	void everyDemoPlanRowPassesPrerequisites(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			int catalogYear = PrerequisiteDao.yearOrNewest(new StudentDao().findCatalogYear(connection, studentId));
			List<PlannedCourse> planned = new PlanEntryDao().findForStudent(connection, studentId);
			assertFalse(planned.isEmpty(), "student " + studentId + " has no plan rows");

			for (PlannedCourse course : planned) {
				List<String> missing = new PrerequisiteCheck().findMissing(connection, studentId,
						course.getCourseId(), course.getTermOrder(), catalogYear);
				assertEquals(List.of(), missing, course.getCode() + " in " + course.getTermLabel());
			}
		}
	}
}
