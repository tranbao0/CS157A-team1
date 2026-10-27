package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// My plan page, AuthFilter makes sure only signed-in students get here
@WebServlet("/plan")
public class PlanServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// PlanPage loads same data as plan card in catalog rail, so page and card always agree
		PlanPage.show(request, response, null);
	}
}
