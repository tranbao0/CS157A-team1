package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// GET shows what parser read from uploaded transcript, POST saves rows student kept
@WebServlet("/transcript/review")
public class TranscriptReviewServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String VIEW = "/WEB-INF/transcript-review.jsp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: read parsed result that TranscriptUploadServlet left in session, nothing there means back to /transcript
		request.getRequestDispatcher(VIEW).forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: insert every checked course into completed in one transaction, roll back all if one fails
		// not built yet, 501 shows error page instead of pretending it worked
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
}
