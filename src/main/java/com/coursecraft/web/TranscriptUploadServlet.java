package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * POST takes transcript PDF from /transcript, parses it, then sends student to /transcript/review.
 * Nothing is saved here, student checks parsed courses first.
 * No doGet here, so GET /transcript/upload gets 405 Method Not Allowed from HttpServlet.
 */
@WebServlet("/transcript/upload")
public class TranscriptUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		/*
		 * TODO: file upload needs @MultipartConfig on this class with size limit,
		 * and form on transcript.jsp needs enctype="multipart/form-data".
		 * Parse major and courses, keep result in session, redirect to /transcript/review.
		 */
		// not built yet, 501 shows error page instead of pretending it worked
		response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
}
