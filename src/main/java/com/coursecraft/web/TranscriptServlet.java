package com.coursecraft.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.coursecraft.model.Major;
import com.coursecraft.model.RemainingAudit;
import com.coursecraft.model.TranscriptCourse;
import com.coursecraft.model.TranscriptGroup;
import com.coursecraft.service.RoadmapService;
import com.coursecraft.service.TranscriptService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/*
 * My Transcript page. GET lists completed courses by category and searches courses not passed yet,
 * POST saves checkboxes, both from the page and from transcript import (js/transcript.js).
 * AuthFilter makes sure only signed-in students get here.
 */
@WebServlet("/transcript")
public class TranscriptServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String VIEW = "/WEB-INF/transcript.jsp";

	// names session key that carries one-time saved message across redirect
	private static final String SAVED_MESSAGE = "transcriptSaved";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// AuthFilter already checked login, so studentId is always there
		int studentId = (Integer) request.getSession().getAttribute("studentId");
		String query = text(request.getParameter("q"));
		String areaKey = text(request.getParameter("area"));

		TranscriptService service = new TranscriptService();
		List<TranscriptCourse> courses;
		List<TranscriptGroup> groups;
		List<Major> majors;
		Major major;
		RemainingAudit audit;
		try {
			courses = service.listCourses(studentId);
			groups = service.group(studentId, courses);
			majors = service.listMajors();
			major = service.findMajor(studentId);
			audit = new RoadmapService().loadAudit(studentId);
		} catch (SQLException e) {
			throw new ServletException("Could not load transcript.", e);
		}

		// search runs only over categories with courses not passed yet, and only once student types or picks one
		List<TranscriptGroup> openGroups = new ArrayList<>();
		boolean anyDone = false;
		for (TranscriptGroup group : groups) {
			if (group.isHasCoursesLeft()) {
				openGroups.add(group);
			}
			if (group.getDone() > 0) {
				anyDone = true;
			}
		}
		boolean searching = !query.isEmpty() || !areaKey.isEmpty();
		List<TranscriptGroup> results = searching ? service.search(openGroups, query, areaKey) : List.of();
		int resultCount = 0;
		for (TranscriptGroup group : results) {
			resultCount += group.getCourses().size();
		}

		// POST leaves message in session, page shows it once and removes it, so Refresh shows no popup
		HttpSession session = request.getSession();
		request.setAttribute("savedMessage", session.getAttribute(SAVED_MESSAGE));
		session.removeAttribute(SAVED_MESSAGE);

		request.setAttribute("query", query);
		request.setAttribute("areaKey", areaKey);
		request.setAttribute("groups", groups);
		request.setAttribute("openGroups", openGroups);
		request.setAttribute("anyDone", anyDone);
		request.setAttribute("searching", searching);
		request.setAttribute("results", results);
		request.setAttribute("resultCount", resultCount);
		request.setAttribute("audit", audit);
		request.setAttribute("coursesJson", coursesJson(courses));
		request.setAttribute("majorsJson", majorsJson(majors));
		request.setAttribute("currentMajorId", major == null ? "" : String.valueOf(major.getMajorId()));
		request.getRequestDispatcher(VIEW).forward(request, response);
	}

	/*
	 * "shown" lists every course on page, "completed" the ticked ones, so unticked means remove.
	 * Import also sends "imported" (courses it newly marked) and "major" (major it found on transcript).
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		List<Integer> shown = numbers(request.getParameterValues("shown"));
		Set<Integer> ticked = new HashSet<>(numbers(request.getParameterValues("completed")));
		String importedText = request.getParameter("imported");
		int imported = importedText != null && importedText.matches("\\d{1,4}") ? Integer.parseInt(importedText) : -1;
		String majorText = request.getParameter("major");
		Integer majorId = majorText != null && majorText.matches("\\d{1,9}") ? Integer.valueOf(majorText) : null;

		String newMajor;
		try {
			newMajor = new TranscriptService().save(studentId, shown, ticked, majorId);
		} catch (SQLException e) {
			throw new ServletException("Could not save transcript.", e);
		}

		String message = imported < 0 ? "Transcript saved."
				: imported == 0 ? "Transcript imported. Nothing new to add."
				: "Transcript imported. " + imported + (imported == 1 ? " course" : " courses") + " marked completed.";
		if (newMajor != null) {
			message = (imported == 0 ? "Transcript imported." : message) + " Major set to " + newMajor + ".";
		}
		request.getSession().setAttribute(SAVED_MESSAGE, message);

		// redirect after POST, so Refresh reloads page instead of sending form again, search stays as it was
		String query = text(request.getParameter("q"));
		String areaKey = text(request.getParameter("area"));
		String back = request.getContextPath() + "/transcript";
		if (!query.isEmpty() || !areaKey.isEmpty()) {
			back += "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
					+ "&area=" + URLEncoder.encode(areaKey, StandardCharsets.UTF_8);
		}
		response.sendRedirect(back);
	}

	// missing parameter becomes empty text, spaces cut
	private String text(String value) {
		return value == null ? "" : value.trim();
	}

	// keeps values that are whole numbers, anything else (hand-made request) is skipped
	private List<Integer> numbers(String[] values) {
		List<Integer> result = new ArrayList<>();
		if (values == null) {
			return result;
		}
		for (String value : values) {
			if (value.matches("\\d{1,9}")) {
				result.add(Integer.valueOf(value));
			}
		}
		return result;
	}

	// every catalog course, so import can match transcript codes to course IDs
	private String coursesJson(List<TranscriptCourse> courses) {
		StringBuilder out = new StringBuilder("[");
		for (TranscriptCourse course : courses) {
			if (out.length() > 1) {
				out.append(',');
			}
			out.append("{\"id\":").append(json(String.valueOf(course.getCourseId())))
					.append(",\"code\":").append(json(course.getCode()))
					.append(",\"passed\":").append(course.isPassed()).append('}');
		}
		return out.append(']').toString();
	}

	// every major with a roadmap, so import can match transcript's plan line
	private String majorsJson(List<Major> majors) {
		StringBuilder out = new StringBuilder("[");
		for (Major major : majors) {
			if (out.length() > 1) {
				out.append(',');
			}
			out.append("{\"id\":").append(json(String.valueOf(major.getMajorId())))
					.append(",\"name\":").append(json(major.getName())).append('}');
		}
		return out.append(']').toString();
	}

	/*
	 * JSON string for inline <script>. Quotes, backslash, < > & and control characters become
	 * unicode escapes, so text cannot close the script tag.
	 */
	private String json(String value) {
		StringBuilder out = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '"' || c == '\\' || c == '<' || c == '>' || c == '&' || c < 0x20) {
				out.append(String.format("\\u%04x", (int) c));
			} else {
				out.append(c);
			}
		}
		return out.append('"').toString();
	}
}