package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.coursecraft.service.PlanGenerateSave;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/plan/generate/save")
public class PlanGenerateSaveServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		List<String> posted = PlanGenerateSave.parseEntries(request.getParameterValues("entry"));
		if (posted == null) {
			sendJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"message\": \"Could not save plan.\"}");
			return;
		}
		// studentId comes from login session, never from request, so student can only save own plan
		int studentId = (Integer) request.getSession().getAttribute("studentId");
		int saved;
		try {
			saved = new PlanGenerateSave().save(studentId, posted);
		} catch (SQLException e) {
			throw new ServletException("Could not save plan.", e);
		}
		if (saved == 0) {
			sendJson(response, HttpServletResponse.SC_CONFLICT,
					"{\"message\": \"Your plan changed. Generate it again.\"}");
			return;
		}
		sendJson(response, HttpServletResponse.SC_OK, "{\"saved\": " + saved + "}");
	}

	private void sendJson(HttpServletResponse response, int status, String json) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
	}
}
