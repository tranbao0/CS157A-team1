package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// POST adds one course to student's completed list, then back to /transcript
// no doGet here, so GET /transcript/add gets 405 Method Not Allowed from HttpServlet
@WebServlet("/transcript/add")
public class TranscriptAddServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: read courseId, check course exists, insert into completed for studentId in session
		// not built yet, 501 shows error page instead of pretending it worked
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
}
