package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import com.coursecraft.model.PlanAddResult;
import com.coursecraft.service.PlanService;
import com.coursecraft.service.UnitRules;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/plan/add")
public class PlanAddServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		int courseId = parseId(request.getParameter("courseId"));
		int sessionId = parseId(request.getParameter("sessionId"));
		if (courseId == -1 || sessionId == -1) {
			sendJson(response, new PlanAddResult(HttpServletResponse.SC_BAD_REQUEST, "Pick course and term to add."));
			return;
		}

		// studentId comes from login session, never from request, so student can only change own plan
		// AuthFilter already checked login, so it is always there
		int studentId = (Integer) request.getSession().getAttribute("studentId");

		PlanAddResult result;
		try {
			result = new PlanService().add(studentId, courseId, sessionId);
		} catch (SQLException e) {
			throw new ServletException("Could not add course to plan.", e);
		}
		sendJson(response, result);
	}

	// turns text into ID, gives -1 when value is missing or not positive whole number
	private int parseId(String value) {
		if (value == null) {
			return -1;
		}
		int id;
		try {
			id = Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
		if (id < 1) {
			return -1;
		}
		return id;
	}

	/*
	 * Writes result as JSON, plan-add.js reads status code to know if it worked.
	 * Every answer has "message". Only 200 adds term, units, unit status, risk level and open seats,
	 * like {"message": "...", "termLabel": "Fall 2026", "termUnits": 15, "unitStatus": "full-time", ...}.
	 */
	private void sendJson(HttpServletResponse response, PlanAddResult result) throws IOException {
		StringBuilder json = new StringBuilder();
		json.append("{\"message\": ");
		json.append(jsonText(result.getMessage()));

		if (result.getStatus() == HttpServletResponse.SC_OK) {
			json.append(", \"termLabel\": ");
			json.append(jsonText(result.getTermLabel()));
			json.append(", \"termUnits\": ");
			json.append(UnitRules.format(result.getTermUnits()));
			json.append(", \"unitStatus\": ");
			json.append(jsonText(result.getUnitStatus()));
			json.append(", \"riskLevel\": ");
			json.append(jsonText(result.getRiskLevel()));
			json.append(", \"openSeats\": ");
			if (result.getOpenSeats() == null) {
				json.append("null");
			} else {
				json.append(result.getOpenSeats());
			}
		}
		json.append("}");

		response.setStatus(result.getStatus());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json.toString());
	}

	// puts text in JSON quotes, null becomes JSON null
	private String jsonText(String text) {
		if (text == null) {
			return "null";
		}
		// backslash and quote would end JSON string early, so escape both
		String safe = text.replace("\\", "\\\\");
		safe = safe.replace("\"", "\\\"");
		return "\"" + safe + "\"";
	}
}
