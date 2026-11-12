package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.model.RemainingAudit;
import com.coursecraft.model.Roadmap;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.RoadmapGeStatus;
import com.coursecraft.util.Db;

// roadmap comparison rules live here, used by both TranscriptServlet and RoadmapServlet
public class RoadmapService {

	// compares student's roadmap against completed table, returns null when student has no major set
	public RemainingAudit loadAudit(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			RoadmapDao dao = new RoadmapDao();
			Roadmap roadmap = dao.findForStudent(connection, studentId);
			if (roadmap == null) {
				return null;
			}

			List<RoadmapCourse> allCourses = dao.findCoursesForStudent(connection, studentId);
			List<RoadmapCourse> remainingCourses = new ArrayList<>();
			int doneCourses = 0;
			for (RoadmapCourse course : allCourses) {
				if (course.isPassed()) {
					doneCourses++;
				} else {
					remainingCourses.add(course);
				}
			}

			List<RoadmapGeStatus> allGe = dao.findGeAreasForStudent(connection, studentId);
			List<RoadmapGeStatus> remainingGe = new ArrayList<>();
			int doneGe = 0;
			for (RoadmapGeStatus area : allGe) {
				if (area.isFulfilled()) {
					doneGe++;
				} else {
					remainingGe.add(area);
				}
			}

			return new RemainingAudit(roadmap, remainingCourses, remainingGe,
					doneCourses, allCourses.size(), doneGe, allGe.size());
		}
	}
}