package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import com.coursecraft.model.PlanRemoveResult;
import com.coursecraft.service.PlanService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// POST removes one entry from student's plan, then back to /plan
// no doGet here, so GET /plan/remove gets 405 Method Not Allowed from HttpServlet
@WebServlet("/plan/remove")
public class PlanRemoveServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// studentId comes from login session, never from request, so student can only change own plan
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		Integer planEntryId = toNumber(request.getParameter("planEntryId"));
		if (planEntryId == null) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			PlanPage.show(request, response, "Pick a course in your plan to remove.");
			return;
		}

		PlanRemoveResult result;
		try {
			result = new PlanService().remove(studentId, planEntryId);
		} catch (SQLException e) {
			throw new ServletException("Could not remove course from plan.", e);
		}

		if (result.getStatus() != HttpServletResponse.SC_OK) {
			// 404 or 409 page keeps student on My plan with red message
			response.setStatus(result.getStatus());
			PlanPage.show(request, response, result.getMessage());
			return;
		}

		// redirect after POST, so Refresh does not try to remove again
		request.getSession().setAttribute(PlanPage.FLASH, result.getMessage());
		response.sendRedirect(request.getContextPath() + "/plan");
	}

	// turns text into whole number, null when missing or not one
	private Integer toNumber(String text) {
		if (text == null) {
			return null;
		}
		try {
			return Integer.valueOf(text.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
