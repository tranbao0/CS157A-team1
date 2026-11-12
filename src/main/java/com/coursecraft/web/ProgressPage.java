package com.coursecraft.web;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.coursecraft.model.PlanTerm;
import com.coursecraft.model.PlannedCourse;
import com.coursecraft.model.RemainingAudit;
import com.coursecraft.model.RoadmapCourse;
import com.coursecraft.model.RoadmapTerm;
import com.coursecraft.model.TranscriptCourse;
import com.coursecraft.service.PlanService;
import jakarta.servlet.http.HttpServletRequest;

// Joins existing read results for one course row per course on My progress.
final class ProgressPage {
    private ProgressPage() {
    }

    static void prepare(HttpServletRequest request, int studentId, List<TranscriptCourse> courses,
            RemainingAudit audit) throws SQLException {
        List<PlanTerm> terms = new PlanService().summarize(studentId);
        Map<Integer, List<PlannedCourse>> plans = new LinkedHashMap<>();
        int planCount = 0;
        for (PlanTerm term : terms) {
            for (PlannedCourse course : term.getCourses()) {
                plans.computeIfAbsent(course.getCourseId(), key -> new ArrayList<>()).add(course);
                planCount++;
            }
        }
        Set<Integer> shown = new HashSet<>();
        if (audit != null) {
            for (RoadmapTerm term : audit.getTerms()) {
                for (RoadmapCourse course : term.getCourses()) {
                    shown.add(course.getCourseId());
                }
            }
        }
        List<TranscriptCourse> other = new ArrayList<>();
        for (TranscriptCourse course : courses) {
            if (plans.containsKey(course.getCourseId()) && shown.add(course.getCourseId())) {
                other.add(course);
            }
        }
        request.setAttribute("completedCourses", courses);
        request.setAttribute("planTerms", terms);
        request.setAttribute("coursePlans", plans);
        request.setAttribute("planCount", planCount);
        request.setAttribute("otherCourses", other);
        request.setAttribute("progressCourseIds", shown);
        request.setAttribute("planMessage", request.getSession().getAttribute(PlanPage.FLASH));
        request.getSession().removeAttribute(PlanPage.FLASH);
    }
}
