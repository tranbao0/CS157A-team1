package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.model.RemainingAudit;
import com.coursecraft.model.Roadmap;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.RoadmapGeStatus;
import com.coursecraft.model.RoadmapTerm;
import com.coursecraft.util.Db;

// roadmap comparison rules live here, used by TranscriptServlet
public class RoadmapService {

	// loads student's term-by-term roadmap grid and all GE areas for transcript page, null when no major is set
	public RemainingAudit loadAudit(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			RoadmapDao dao = new RoadmapDao();
			Roadmap roadmap = dao.findForStudent(connection, studentId);
			if (roadmap == null) {
				return null;
			}

			List<RoadmapCourse> allCourses = dao.findCoursesForStudent(connection, studentId);
			Map<Integer, RoadmapTerm> byTerm = new LinkedHashMap<>();
			int doneCourses = 0;
			int totalCourses = 0;
			int doneElectives = 0;
			int totalElectives = 0;

			for (RoadmapCourse course : allCourses) {
				if ("Elective".equalsIgnoreCase(course.getRequirementType())) {
					totalElectives++;
					if (course.isPassed()) {
						doneElectives++;
					}
				} else {
					totalCourses++;
					if (course.isPassed()) {
						doneCourses++;
					}
				}
				RoadmapTerm bucket = byTerm.computeIfAbsent(course.getSuggestedTerm(), RoadmapTerm::new);
				bucket.addCourse(course);
			}

			List<RoadmapGeStatus> allGe = dao.findGeAreasForStudent(connection, studentId);
			int doneGe = 0;
			for (RoadmapGeStatus area : allGe) {
				if (area.isFulfilled()) {
					doneGe++;
				}
			}

			return new RemainingAudit(
					roadmap,
					new ArrayList<>(byTerm.values()),
					allGe,
					doneCourses,
					totalCourses,
					doneElectives,
					totalElectives,
					doneGe,
					allGe.size());
		}
	}
}