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

// My plan page, AuthFilter makes sure only signed-in students get here
@WebServlet("/plan")
public class PlanServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String PLAN_VIEW = "/WEB-INF/plan.jsp";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// AuthFilter already checked login, so studentId is always there
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		// loads same data as plan card in catalog rail, so page and card always agree
		List<PlanTerm> terms;
		try {
			terms = new PlanService().summarize(studentId);
		} catch (SQLException e) {
			throw new ServletException("Could not load plan.", e);
		}
		request.setAttribute("planTerms", terms);
		// plan-summary.jsp reads this and shows full-page version instead of rail card
		request.setAttribute("planView", "page");
		request.getRequestDispatcher(PLAN_VIEW).forward(request, response);
	}
}
