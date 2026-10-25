package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// POST builds term-by-term plan from completed courses, roadmap, grad term and unit cap
// no doGet here, so GET /plan/generate gets 405 Method Not Allowed from HttpServlet
@WebServlet("/plan/generate")
public class PlanGenerateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: needs major and grad term from /profile, write all new plan_entries in one transaction
		// not built yet, 501 shows error page instead of pretending it worked
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
}
