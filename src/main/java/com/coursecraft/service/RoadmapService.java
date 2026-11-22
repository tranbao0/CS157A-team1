package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import com.coursecraft.dao.RoadmapDao;
import com.coursecraft.dao.RoadmapDao.ElectiveGroupRow;
import com.coursecraft.dao.RoadmapDao.ElectiveSlotRow;
import com.coursecraft.dao.RoadmapDao.GroupTakenCourse;
import com.coursecraft.model.ElectiveGroupStatus;
import com.coursecraft.model.RemainingAudit;
import com.coursecraft.model.Roadmap;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.RoadmapElectiveSlot;
import com.coursecraft.model.RoadmapGeStatus;
import com.coursecraft.model.RoadmapTerm;
import com.coursecraft.util.Db;

// roadmap comparison rules live here, used by TranscriptServlet
public class RoadmapService {

	// loads student's term by term roadmap grid, elective group rules, and all GE areas for transcript page
	public RemainingAudit loadAudit(int studentId) throws SQLException {
		try (Connection connection = Db.getConnection()) {
			RoadmapDao dao = new RoadmapDao();
			Roadmap roadmap = dao.findForStudent(connection, studentId);
			if (roadmap == null) {
				return null;
			}

			Map<Integer, RoadmapTerm> byTerm = new TreeMap<>();
			int doneCourses = 0;
			int totalCourses = 0;

			for (RoadmapCourse course : dao.findCoursesForStudent(connection, studentId)) {
				totalCourses++;
				if (course.isPassed()) {
					doneCourses++;
				}
				RoadmapTerm bucket = byTerm.computeIfAbsent(course.getSuggestedTerm(), RoadmapTerm::new);
				bucket.addCourse(course);
			}

			List<ElectiveGroupRow> groupRows = dao.findElectiveGroupsForStudent(connection, studentId);
			List<GroupTakenCourse> takenRows = dao.findTakenElectivesForStudent(connection, studentId);
			Map<Integer, List<RoadmapCourse>> coursesByGroup = new HashMap<>();
			for (GroupTakenCourse item : takenRows) {
				coursesByGroup.computeIfAbsent(item.groupId, k -> new ArrayList<>()).add(item.course);
			}

			List<ElectiveGroupStatus> electiveGroups = new ArrayList<>();
			Map<Integer, Queue<RoadmapCourse>> slotFillQueues = new HashMap<>();
			for (ElectiveGroupRow row : groupRows) {
				List<RoadmapCourse> matched = coursesByGroup.getOrDefault(row.groupId, List.of());
				double doneUnits = 0;
				for (RoadmapCourse rc : matched) {
					try {
						doneUnits += Double.parseDouble(rc.getUnits());
					} catch (NumberFormatException ignored) {
					}
				}
				electiveGroups.add(new ElectiveGroupStatus(
						row.groupId, row.name, row.minUnits, row.minCourses, doneUnits, matched.size()));
				slotFillQueues.put(row.groupId, new LinkedList<>(matched));
			}

			for (ElectiveSlotRow slot : dao.findElectiveSlotsForStudent(connection, studentId)) {
				Queue<RoadmapCourse> queue = slotFillQueues.get(slot.groupId);
				RoadmapCourse filled = (queue != null && !queue.isEmpty()) ? queue.poll() : null;
				RoadmapElectiveSlot resolved = new RoadmapElectiveSlot(
						slot.slotId, slot.groupId, slot.groupName, slot.suggestedTerm, slot.units, filled);
				byTerm.computeIfAbsent(slot.suggestedTerm, RoadmapTerm::new).addElectiveSlot(resolved);
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
					electiveGroups,
					allGe,
					doneCourses,
					totalCourses,
					doneGe,
					allGe.size());
		}
	}
}