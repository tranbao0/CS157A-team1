package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.coursecraft.model.PlanTerm;
import com.coursecraft.service.PlanService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// GET gives plan card HTML, plan-add.js loads it after each add so card updates without page reload
@WebServlet("/plan/summary")
public class PlanSummaryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// catalog.jsp includes same file, so card looks same on page load and after add
	private static final String SUMMARY_VIEW = "/WEB-INF/plan-summary.jsp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// AuthFilter already checked login, so studentId is always there
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		List<PlanTerm> terms;
		try {
			terms = new PlanService().summarize(studentId);
		} catch (SQLException e) {
			throw new ServletException("Could not load plan.", e);
		}
		request.setAttribute("planTerms", terms);
		request.getRequestDispatcher(SUMMARY_VIEW).forward(request, response);
	}
}
