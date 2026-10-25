package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// POST changes one completed row, then back to /transcript
// no doGet here, so GET /transcript/edit gets 405 Method Not Allowed from HttpServlet
@WebServlet("/transcript/edit")
public class TranscriptEditServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: read courseId and taken_and_passed, update row for studentId in session only
		// not built yet, 501 shows error page instead of pretending it worked
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
}
