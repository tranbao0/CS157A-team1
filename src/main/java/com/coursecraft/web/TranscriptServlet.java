package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// My courses page, lists completed courses with add, edit, remove and upload forms, AuthFilter makes sure only signed-in students get here
@WebServlet("/transcript")
public class TranscriptServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String VIEW = "/WEB-INF/transcript.jsp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO: load student's rows from completed joined with courses, only for studentId in session
		request.getRequestDispatcher(VIEW).forward(request, response);
	}
}
