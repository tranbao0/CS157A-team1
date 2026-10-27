package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import com.coursecraft.model.StudentProfile;
import com.coursecraft.service.ProfileService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// GET shows profile form (major, grad term, placement), POST saves it
// roadmap page, plan generation and prerequisite checks all read these values
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String VIEW = "/WEB-INF/profile.jsp";

	// names session key that carries one-time "Profile saved." message across redirect
	private static final String SAVED_FLAG = "profileSaved";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// AuthFilter already checked login, so studentId is always there
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		StudentProfile profile;
		try {
			profile = new ProfileService().load(studentId);
		} catch (SQLException e) {
			throw new ServletException("Could not load profile.", e);
		}

		// POST leaves flag in session, page shows message once and removes flag, so Refresh shows clean form
		HttpSession session = request.getSession();
		boolean saved = session.getAttribute(SAVED_FLAG) != null;
		session.removeAttribute(SAVED_FLAG);
		request.setAttribute("saved", saved);

		showForm(request, response, profile, profile, null, null);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		// first problem found wins, text that is not number keeps field empty in submitted profile
		String error = null;

		Integer roadmapId = null;
		String roadmapText = trimmed(request.getParameter("roadmapId"));
		if (roadmapText != null) {
			roadmapId = toNumber(roadmapText);
			if (roadmapId == null) {
				error = "Pick your major from the list.";
			}
		}

		Integer termId = null;
		String termText = trimmed(request.getParameter("termId"));
		if (termText != null) {
			termId = toNumber(termText);
			if (termId == null && error == null) {
				error = "Pick a graduation term from the list.";
			}
		}

		Integer ppaScore = null;
		String ppaText = trimmed(request.getParameter("ppaScore"));
		if (ppaText != null) {
			ppaScore = toNumber(ppaText);
			if (ppaScore == null && error == null) {
				error = "PPA score must be a whole number from 0 to 100.";
			}
		}

		StudentProfile submitted = new StudentProfile(roadmapId, termId, ppaScore,
				trimmed(request.getParameter("mathCategory")), trimmed(request.getParameter("writingCategory")));

		ProfileService service = new ProfileService();
		StudentProfile current;
		try {
			current = service.load(studentId);
			if (error == null) {
				error = service.check(submitted, current.getGradTermId());
			}
			if (error == null) {
				service.save(studentId, submitted);
			}
		} catch (SQLException e) {
			throw new ServletException("Could not save profile.", e);
		}

		if (error != null) {
			// 400 tells browser and tests form was refused, page keeps what student typed
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			showForm(request, response, submitted, current, ppaText, error);
			return;
		}

		// get-started dialog on catalog sends returnTo=catalog, so student lands back where they were
		// fixed value check, never redirect to address taken from request
		if ("catalog".equals(request.getParameter("returnTo"))) {
			response.sendRedirect(request.getContextPath() + "/catalog.jsp");
			return;
		}

		// redirect after POST, so Refresh reloads page instead of sending form again
		request.getSession().setAttribute(SAVED_FLAG, Boolean.TRUE);
		response.sendRedirect(request.getContextPath() + "/profile");
	}

	/*
	 * Loads dropdown lists and forwards to form.
	 * shown fills fields, saved is what is on file (its grad term stays in term list even when over).
	 * ppaText keeps typed PPA text after error, since text that is not number cannot live in Integer.
	 */
	private void showForm(HttpServletRequest request, HttpServletResponse response, StudentProfile shown,
			StudentProfile saved, String ppaText, String error) throws ServletException, IOException {
		ProfileService service = new ProfileService();
		try {
			request.setAttribute("roadmaps", service.listRoadmaps());
			request.setAttribute("terms", service.listTerms(saved.getGradTermId()));
			request.setAttribute("categories", service.listCategories());
		} catch (SQLException e) {
			throw new ServletException("Could not load profile.", e);
		}
		request.setAttribute("profile", shown);
		request.setAttribute("ppaText", ppaText);
		request.setAttribute("error", error);
		request.getRequestDispatcher(VIEW).forward(request, response);
	}

	// cuts spaces, empty field or "Not set" choice becomes null
	private String trimmed(String value) {
		if (value == null) {
			return null;
		}
		String result = value.trim();
		if (result.isEmpty()) {
			return null;
		}
		return result;
	}

	// turns text into whole number, null when text is not one
	private Integer toNumber(String text) {
		try {
			return Integer.valueOf(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
