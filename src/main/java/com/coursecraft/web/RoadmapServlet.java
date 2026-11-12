package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import com.coursecraft.model.RoadmapPageData;
import com.coursecraft.service.RoadmapService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Roadmap page, required courses and GE areas for student's major, each marked done or remaining, AuthFilter makes sure only signed-in students get here
@WebServlet("/roadmap")
public class RoadmapServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String VIEW = "/WEB-INF/roadmap.jsp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		RoadmapPageData roadmapData;
		try {
			roadmapData = new RoadmapService().loadFullRoadmap(studentId);
		} catch (SQLException e) {
			throw new ServletException("Could not load roadmap.", e);
		}

		request.setAttribute("roadmapData", roadmapData);
		request.getRequestDispatcher(VIEW).forward(request, response);
	}
}