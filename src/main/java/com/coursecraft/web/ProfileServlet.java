package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// GET shows major and grad term form, POST saves them
// roadmap page and plan generation both need these two values
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String VIEW = "/WEB-INF/profile.jsp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: load majors with their roadmaps and terms for dropdowns, plus student's current picks
		request.getRequestDispatcher(VIEW).forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: check roadmapId and termId exist, update students row for studentId in session
		// not built yet, 501 shows error page instead of pretending it worked
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
}
