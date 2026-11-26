package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.coursecraft.model.GeneratedPlan;
import com.coursecraft.model.ProposedEntry;
import com.coursecraft.model.ProposedTerm;
import com.coursecraft.model.UnplacedCourse;
import com.coursecraft.service.PlanGenerator;
import com.coursecraft.service.PlanService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/plan/generate")
public class PlanGenerateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("text/html")) {
			response.sendRedirect(request.getContextPath() + "/plan");
			return;
		}
		int studentId = (Integer) request.getSession().getAttribute("studentId");
		GeneratedPlan plan;
		try {
			plan = new PlanGenerator().preview(studentId);
		} catch (SQLException e) {
			throw new ServletException("Could not generate plan.", e);
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(toJson(plan, studentId));
	}

	static String toJson(GeneratedPlan plan, int studentId) {
		StringBuilder json = new StringBuilder("{\"terms\": [");
		for (int i = 0; i < plan.getTerms().size(); i++) {
			if (i > 0) {
				json.append(", ");
			}
			ProposedTerm term = plan.getTerms().get(i);
			json.append("{\"termId\": ");
			json.append(term.getTermId());
			json.append(", \"termOrder\": ");
			json.append(term.getTermOrder());
			json.append(", \"gradTerm\": ");
			json.append(term.isGradTerm());
			json.append(", \"label\": ");
			json.append(jsonText(term.getLabel()));
			json.append(", \"units\": ");
			json.append(term.getUnits());
			json.append(", \"unitsText\": ");
			json.append(jsonText(term.getUnitsText()));
			json.append(", \"unitWarningTitle\": ");
			json.append(jsonText(term.getUnitWarningTitle()));
			json.append(", \"unitWarningText\": ");
			json.append(jsonText(term.getUnitWarningText()));
			json.append(", \"unitWarningKind\": ");
			json.append(jsonText(term.getUnitWarningKind()));
			String warningKey = null;
			if (term.getUnitWarningKind() != null) {
				warningKey = PlanService.unitWarningKey(studentId, term.getTermId(), term.getUnitStatus());
			}
			json.append(", \"unitWarningKey\": ");
			json.append(jsonText(warningKey));
			json.append(", \"unitStatus\": ");
			json.append(jsonText(term.getUnitStatus()));
			json.append(", \"unitCap\": ");
			json.append(term.getUnitCap());
			json.append(", \"entries\": [");
			for (int j = 0; j < term.getEntries().size(); j++) {
				if (j > 0) {
					json.append(", ");
				}
				appendEntry(json, term.getEntries().get(j));
			}
			json.append("], \"warnings\": ");
			appendWarnings(json, term.getWarnings());
			json.append("}");
		}
		json.append("], \"unplaced\": [");
		for (int i = 0; i < plan.getUnplaced().size(); i++) {
			if (i > 0) {
				json.append(", ");
			}
			UnplacedCourse course = plan.getUnplaced().get(i);
			json.append("{\"courseId\": ");
			json.append(course.getCourseId());
			json.append(", \"code\": ");
			json.append(jsonText(course.getCode()));
			json.append(", \"units\": ");
			json.append(course.getUnits());
			json.append(", \"unitsText\": ");
			json.append(jsonText(String.valueOf(course.getUnits())));
			json.append(", \"reason\": ");
			json.append(jsonText(course.getReason()));
			json.append("}");
		}
		json.append("], \"warnings\": ");
		appendWarnings(json, plan.getWarnings());
		json.append("}");
		return json.toString();
	}

	private static void appendEntry(StringBuilder json, ProposedEntry entry) {
		json.append("{\"courseId\": ");
		json.append(entry.getCourseId());
		json.append(", \"code\": ");
		json.append(jsonText(entry.getCode()));
		json.append(", \"units\": ");
		json.append(entry.getUnits());
		json.append(", \"unitsText\": ");
		json.append(jsonText(String.valueOf(entry.getUnits())));
		json.append(", \"sessionId\": ");
		json.append(entry.getSessionId());
		json.append(", \"title\": ");
		json.append(jsonText(entry.getTitle()));
		json.append(", \"suggested\": ");
		json.append(entry.isSuggested());
		json.append("}");
	}

	private static void appendWarnings(StringBuilder json, List<String> warnings) {
		json.append("[");
		for (int i = 0; i < warnings.size(); i++) {
			if (i > 0) {
				json.append(", ");
			}
			json.append(jsonText(warnings.get(i)));
		}
		json.append("]");
	}

	private static String jsonText(String text) {
		if (text == null) {
			return "null";
		}
		StringBuilder json = new StringBuilder("\"");
		for (int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			if (character == '\\' || character == '"') {
				json.append('\\');
				json.append(character);
			} else if (character < 0x20) {
				json.append("\\u00");
				String hex = Integer.toHexString(character);
				if (hex.length() == 1) {
					json.append('0');
				}
				json.append(hex);
			} else {
				json.append(character);
			}
		}
		json.append('"');
		return json.toString();
	}
}
