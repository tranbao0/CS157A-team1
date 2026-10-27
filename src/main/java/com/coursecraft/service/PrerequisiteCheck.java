package com.coursecraft.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.coursecraft.dao.PlanEntryDao;
import com.coursecraft.dao.PrerequisiteDao;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.PrerequisiteGroup;

/*
 * Checks prerequisites against student's plan, where term of each planned course matters.
 * PrerequisiteDao gives open groups (no completed course or placement meets them yet),
 * PlanEntryDao gives planned courses with term order, this class checks them against each other.
 * Planned course meets group only when its term comes before other course's term, same term does not count.
 */
public class PrerequisiteCheck {

	/*
	 * Lists groups student still misses for course in target term, one string per group.
	 * Each string lists choices like "MATH 19 or placement M-I (PPA 70+), M-II (PPA 70+)".
	 * Empty list means all met, plan add then goes on to unit cap check.
	 */
	public List<String> findMissing(Connection connection, int studentId, int courseId, int targetTermOrder,
			int catalogYear) throws SQLException {
		PrerequisiteDao prerequisiteDao = new PrerequisiteDao();
		List<PrerequisiteGroup> groups = prerequisiteDao.findOpenGroupsForCourse(connection, studentId, courseId,
				catalogYear);
		List<PlannedCourse> planned = new PlanEntryDao().findForStudent(connection, studentId);

		List<String> missing = new ArrayList<>();
		for (PrerequisiteGroup group : groups) {
			if (!isMetByPlan(group, planned, targetTermOrder, 0)) {
				String choices = String.join(" or ", group.getOptionCodes());
				List<String> placements = prerequisiteDao.findPlacementOptions(connection, group.getGroupId());
				if (!placements.isEmpty()) {
					String placementText = "placement " + String.join(", ", placements);
					// placement-only group has no course options, so "or" goes in only when both exist
					if (choices.isEmpty()) {
						choices = placementText;
					} else {
						choices = choices + " or " + placementText;
					}
				}
				missing.add(choices);
			}
		}
		return missing;
	}

	/*
	 * Lists later planned courses that would lose prerequisite if removed course left plan, A to Z.
	 * Later course counts only when removed course sits in earlier term (so it really meets group)
	 * and no other option of that group is planned before later course's term.
	 */
	public List<String> findDependents(Connection connection, int studentId, int removedCourseId, int catalogYear)
			throws SQLException {
		List<PrerequisiteGroup> groups = new PrerequisiteDao().findOpenGroups(connection, studentId, catalogYear);
		List<PlannedCourse> planned = new PlanEntryDao().findForStudent(connection, studentId);

		// TreeSet keeps each code once and sorts them A to Z
		Set<String> codes = new TreeSet<>();
		for (PrerequisiteGroup group : groups) {
			if (group.hasOption(removedCourseId)) {
				for (PlannedCourse later : planned) {
					if (later.getCourseId() == group.getCourseId()
							&& isPlannedBefore(planned, removedCourseId, later.getTermOrder())
							&& !isMetByPlan(group, planned, later.getTermOrder(), removedCourseId)) {
						codes.add(later.getCode());
					}
				}
			}
		}
		return new ArrayList<>(codes);
	}

	/*
	 * Marks planned courses whose prerequisites run through petition term,
	 * so plan card can say what breaks if petition is denied.
	 * Petition term is Fall or Spring term with more than normal limit (19) units planned.
	 * Walk starts with every course in petition term, then keeps adding later planned courses
	 * that need walked course through open group, until no new course joins.
	 * That way chains like CS 46A -> CS 46B -> CS 47 are found.
	 */
	public void markPetitionDependents(Connection connection, int studentId, int catalogYear,
			List<PlannedCourse> planned) throws SQLException {
		List<PrerequisiteGroup> groups = new PrerequisiteDao().findOpenGroups(connection, studentId, catalogYear);

		// course ID -> petition terms it depends on, LinkedHashSet keeps them in term order
		Map<Integer, Set<String>> petitionLabels = new HashMap<>();
		// course ID -> earlier courses that link it to petition term, TreeSet sorts them A to Z
		Map<Integer, Set<String>> throughCodes = new HashMap<>();

		for (int petitionTermId : findPetitionTermIds(planned)) {
			String petitionLabel = null;
			// toWalk holds courses whose later dependents are not checked yet
			List<PlannedCourse> toWalk = new ArrayList<>();
			Set<Integer> reached = new HashSet<>();
			for (PlannedCourse course : planned) {
				if (course.getTermId() == petitionTermId) {
					toWalk.add(course);
					reached.add(course.getCourseId());
					petitionLabel = course.getTermLabel();
				}
			}

			while (!toWalk.isEmpty()) {
				PlannedCourse earlier = toWalk.remove(0);
				for (PlannedCourse later : planned) {
					if (later.getTermOrder() > earlier.getTermOrder()
							&& needs(groups, later.getCourseId(), earlier.getCourseId())) {
						int laterId = later.getCourseId();
						if (!petitionLabels.containsKey(laterId)) {
							petitionLabels.put(laterId, new LinkedHashSet<>());
							throughCodes.put(laterId, new TreeSet<>());
						}
						petitionLabels.get(laterId).add(petitionLabel);
						throughCodes.get(laterId).add(earlier.getCode());

						// each course gets walked once, even when several earlier courses lead to it
						if (!reached.contains(laterId)) {
							reached.add(laterId);
							toWalk.add(later);
						}
					}
				}
			}
		}

		for (PlannedCourse course : planned) {
			int courseId = course.getCourseId();
			if (petitionLabels.containsKey(courseId)) {
				course.setPetitionDependency(String.join(" and ", petitionLabels.get(courseId)),
						String.join(", ", throughCodes.get(courseId)));
			}
		}
	}

	// adds up units of each Fall and Spring term, keeps terms over normal limit, oldest first
	private List<Integer> findPetitionTermIds(List<PlannedCourse> planned) {
		// LinkedHashMap keeps terms in same order as planned list, which is oldest first
		Map<Integer, Double> unitsByTerm = new LinkedHashMap<>();
		for (PlannedCourse course : planned) {
			if ("Fall".equals(course.getSeason()) || "Spring".equals(course.getSeason())) {
				double units = course.getUnits();
				if (unitsByTerm.containsKey(course.getTermId())) {
					units = units + unitsByTerm.get(course.getTermId());
				}
				unitsByTerm.put(course.getTermId(), units);
			}
		}

		List<Integer> termIds = new ArrayList<>();
		for (int termId : unitsByTerm.keySet()) {
			if (unitsByTerm.get(termId) > UnitRules.NORMAL_LIMIT) {
				termIds.add(termId);
			}
		}
		return termIds;
	}

	/*
	 * Tells if some option of group is planned in term before beforeTermOrder.
	 * skipCourseId is left out of check, plan remove passes course being removed.
	 * Pass 0 to skip nothing, no course has ID 0.
	 */
	private boolean isMetByPlan(PrerequisiteGroup group, List<PlannedCourse> planned, int beforeTermOrder,
			int skipCourseId) {
		for (PlannedCourse course : planned) {
			if (course.getCourseId() != skipCourseId && group.hasOption(course.getCourseId())
					&& course.getTermOrder() < beforeTermOrder) {
				return true;
			}
		}
		return false;
	}

	// tells if course is planned in term before beforeTermOrder
	private boolean isPlannedBefore(List<PlannedCourse> planned, int courseId, int beforeTermOrder) {
		for (PlannedCourse course : planned) {
			if (course.getCourseId() == courseId && course.getTermOrder() < beforeTermOrder) {
				return true;
			}
		}
		return false;
	}

	// tells if later course has open group that earlier course counts towards
	private boolean needs(List<PrerequisiteGroup> groups, int laterCourseId, int earlierCourseId) {
		for (PrerequisiteGroup group : groups) {
			if (group.getCourseId() == laterCourseId && group.hasOption(earlierCourseId)) {
				return true;
			}
		}
		return false;
	}
}
