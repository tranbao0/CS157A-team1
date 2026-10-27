package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import com.coursecraft.service.PlanService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/*
 * Draws My plan page for PlanServlet (normal GET) and PlanRemoveServlet (refused remove).
 * Both need same data and same JSP, so it lives in one place.
 * Message after successful remove travels in session across redirect and shows once.
 */
public final class PlanPage {

	// inside WEB-INF so browser cant open it directly, only servlets can forward to it
	private static final String VIEW = "/WEB-INF/plan.jsp";

	// names session key that carries one-time success message across redirect
	public static final String FLASH = "planMessage";

	private PlanPage() {
	}

	// shows plan, error is null on normal load, shown in red banner when remove was refused
	public static void show(HttpServletRequest request, HttpServletResponse response, String error)
			throws ServletException, IOException {
		// AuthFilter already checked login, so studentId is always there
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		try {
			request.setAttribute("planTerms", new PlanService().summarize(studentId));
		} catch (SQLException e) {
			throw new ServletException("Could not load plan.", e);
		}
		// plan-summary.jsp reads this and shows full-page version instead of rail card
		request.setAttribute("planView", "page");

		// takes success message out of session, so Refresh does not show it again
		HttpSession session = request.getSession();
		request.setAttribute("planMessage", session.getAttribute(FLASH));
		session.removeAttribute(FLASH);
		request.setAttribute("planError", error);

		request.getRequestDispatcher(VIEW).forward(request, response);
	}
}
