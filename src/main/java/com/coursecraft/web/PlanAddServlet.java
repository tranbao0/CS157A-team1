package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// POST adds course to plan for term, write path from requirements doc
// no doGet here, so GET /plan/add gets 405 Method Not Allowed from HttpServlet
@WebServlet("/plan/add")
public class PlanAddServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/*
		 * TODO: steps from requirements doc
		 * 1. validate courseId and term, session must own plan
		 * 2. load completed courses and planned units for term
		 * 3. load prerequisite groups and roadmap term position
		 * 4. prereqs missing gives 422 naming them, unit cap over gives 409
		 * 5. otherwise insert plan_entries row, answer 200 with bottleneck label and open seats
		 */
		// not built yet, 501 shows error page instead of pretending it worked
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
}
