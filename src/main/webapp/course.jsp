<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.sql.Connection, java.sql.PreparedStatement, java.sql.ResultSet, java.sql.ResultSetMetaData, java.sql.SQLException" %>
<%@ page import="java.util.ArrayList, java.util.LinkedHashMap, java.util.List, java.util.Map" %>
<%@ page import="com.coursecraft.util.Db" %>
<%-- Course detail. Opens from the catalog with ?id=<course_id>&section=<section_id>. --%>
<%-- TODO: move the queries to a servlet --%>
<%!
	// Escapes HTML.
	private static String esc(String value) {
		if (value == null) return "";
		StringBuilder out = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '&':  out.append("&amp;");  break;
				case '<':  out.append("&lt;");   break;
				case '>':  out.append("&gt;");   break;
				case '"':  out.append("&quot;"); break;
				case '\'': out.append("&#39;");  break;
				default:   out.append(c);
			}
		}
		return out.toString();
	}

	// "-" for a value that has no snapshot yet.
	private static String orDash(String value) {
		return value == null ? "-" : esc(value);
	}

	// Runs a SELECT, returns each row as column label -> text. Values are bound, never concatenated.
	private static List<Map<String, String>> select(Connection connection, String sql, Object... params)
			throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				statement.setObject(i + 1, params[i]);
			}
			try (ResultSet result = statement.executeQuery()) {
				ResultSetMetaData meta = result.getMetaData();
				List<Map<String, String>> rows = new ArrayList<>();
				while (result.next()) {
					Map<String, String> row = new LinkedHashMap<>();
					for (int i = 1; i <= meta.getColumnCount(); i++) {
						row.put(meta.getColumnLabel(i), result.getString(i));
					}
					rows.add(row);
				}
				return rows;
			}
		}
	}
%>
<%
	String context = request.getContextPath();

	// LoginSession puts both in the session. studentId is what the completed table is keyed on.
	String email = (String) session.getAttribute("email");
	Integer studentId = (Integer) session.getAttribute("studentId");
	boolean signedIn = email != null;
	boolean canEdit = studentId != null;

	String loadError = null;
	int status = 200;
	Map<String, String> course = null;
	List<List<Map<String, String>>> groups = new ArrayList<>();
	List<Map<String, String>> dependents = new ArrayList<>();
	Map<String, String> section = null;
	Map<String, String> bottleneck = null;
	boolean completed = false;

	int courseId = -1;
	try {
		courseId = Integer.parseInt(request.getParameter("id"));
	} catch (NumberFormatException e) {
		loadError = "No course selected.";
		status = 400;
	}

	if (loadError == null) {
		// One connection for every query. Closes on failure too.
		try (Connection connection = Db.getConnection()) {

			// The Completed checkbox posts here. Ticked sends "completed", unticked sends nothing.
			if ("POST".equals(request.getMethod())) {
				if (canEdit) {
					String sql = request.getParameter("completed") != null
						? "INSERT INTO completed (student_id, course_id, taken_and_passed) VALUES (?, ?, 1) "
							+ "ON DUPLICATE KEY UPDATE taken_and_passed = 1"
						: "DELETE FROM completed WHERE student_id = ? AND course_id = ?";
					try (PreparedStatement statement = connection.prepareStatement(sql)) {
						statement.setInt(1, studentId);
						statement.setInt(2, courseId);
						statement.executeUpdate();
					}
				}
				// Back to the same course and section, so a refresh does not repeat the POST.
				String back = context + "/course.jsp?id=" + courseId;
				String sectionParam = request.getParameter("section");
				if (sectionParam != null && sectionParam.matches("\\d+")) {
					back += "&section=" + sectionParam;
				}
				response.sendRedirect(back);
				return;
			}

			// Course and GE area. ge_area_code can be NULL, so LEFT JOIN is used to account for NULL values.
			List<Map<String, String>> found = select(connection,
				"SELECT c.course_id, c.subject, c.course_number, c.title, c.units, c.description, "
				+ "       c.ge_area_code, g.name AS ge_name "
				+ "FROM courses c LEFT JOIN ge_areas g ON g.code = c.ge_area_code "
				+ "WHERE c.course_id = ?", courseId);

			if (found.isEmpty()) {
				loadError = "That course does not exist.";
				status = 404;
			} else {
				course = found.get(0);

				// Prerequisites. Groups are ANDed, courses inside a group are ORed.
				List<Map<String, String>> prereqRows = select(connection,
					"SELECT pg.group_id, c.course_id, c.subject, c.course_number "
					+ "FROM prerequisite_groups pg "
					+ "JOIN counts_towards ct ON ct.group_id = pg.group_id "
					+ "JOIN courses c ON c.course_id = ct.course_id "
					+ "WHERE pg.course_id = ? ORDER BY pg.group_id, c.subject, c.course_number", courseId);
				Map<String, List<Map<String, String>>> byGroup = new LinkedHashMap<>();
				for (Map<String, String> row : prereqRows) {
					List<Map<String, String>> g = byGroup.get(row.get("group_id"));
					if (g == null) {
						g = new ArrayList<>();
						byGroup.put(row.get("group_id"), g);
					}
					g.add(row);
				}
				groups.addAll(byGroup.values());

				// Courses that need this one as a prerequisite.
				dependents = select(connection,
					"SELECT DISTINCT c.course_id, c.subject, c.course_number "
					+ "FROM counts_towards ct "
					+ "JOIN prerequisite_groups pg ON pg.group_id = ct.group_id "
					+ "JOIN courses c ON c.course_id = pg.course_id "
					+ "WHERE ct.course_id = ? ORDER BY c.subject, c.course_number", courseId);

				// Sections with their latest enrollment snapshot. Active first, newest term first.
				List<Map<String, String>> sections = select(connection,
					"SELECT s.section_id, s.section_code, s.class_number, s.status, "
					+ "       es.seat_capacity, es.open_seats, es.waitlist_count "
					+ "FROM sections s "
					+ "JOIN sessions se ON se.session_id = s.session_id "
					+ "JOIN terms t ON t.term_id = se.term_id "
					+ "LEFT JOIN enrollment_snapshots es ON es.section_id = s.section_id "
					+ "  AND es.captured_at = (SELECT MAX(captured_at) FROM enrollment_snapshots "
					+ "                        WHERE section_id = s.section_id) "
					+ "WHERE s.course_id = ? ORDER BY s.status, t.term_id DESC, s.section_code", courseId);

				// The section the catalog linked to. With none (a prerequisite link), use the first.
				String wanted = request.getParameter("section");
				for (Map<String, String> s : sections) {
					if (s.get("section_id").equals(wanted)) {
						section = s;
						break;
					}
				}
				if (section == null && !sections.isEmpty()) {
					section = sections.get(0);
				}

				// Latest risk level, same rule as the catalog.
				List<Map<String, String>> bn = select(connection,
					"SELECT b.risk_level "
					+ "FROM bottleneck_scores b JOIN terms t ON t.term_id = b.term_id "
					+ "WHERE b.course_id = ? ORDER BY t.term_id DESC, b.computed_at DESC LIMIT 1", courseId);
				bottleneck = bn.isEmpty() ? null : bn.get(0);

				// Is it on this student's completed list as passed?
				if (canEdit) {
					List<Map<String, String>> done = select(connection,
						"SELECT taken_and_passed FROM completed WHERE student_id = ? AND course_id = ?",
						studentId, courseId);
					completed = !done.isEmpty() && "1".equals(done.get(0).get("taken_and_passed"));
				}
			}
		} catch (SQLException e) {
			getServletContext().log("Course detail query failed", e);
			loadError = "Could not read the course. Check that MySQL is running.";
			status = 500;
		}
	}

	if (status != 200) {
		response.setStatus(status);
	}

	// CS 146 Sec-01 Data Structures and Algorithms. Code gets the marker, like the term on the catalog.
	String code = "Course";
	String rest = "";
	if (course != null) {
		code = course.get("subject") + " " + course.get("course_number");
		rest = (section == null ? "" : " Sec-" + section.get("section_code")) + " " + course.get("title");
	}
%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title><%= esc(code + rest) %> - CourseCraft</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700;800&display=swap">
	<link rel="stylesheet" href="<%= context %>/css/app.css">
	<link rel="stylesheet" href="<%= context %>/css/motion.css">
</head>
<body>

<a class="skip-link" href="#main">Skip to course details</a>

<%-- site header: shared top bar plus this page's head, slides away together on scroll (header.js) --%>
<% request.setAttribute("navPage", "catalog"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
	<div class="pagehead">
		<div class="pagehead__text">
			<p class="pagehead__back"><a class="btn btn--sm" href="<%= context %>/catalog.jsp">&larr; Back</a></p>
<% if (loadError == null) { %>
			<div class="titlerow">
				<h1 class="pagehead__title"><span class="pagehead__term"><%= esc(code) %></span><%= esc(rest) %></h1>
<% if (bottleneck == null) { %>
				<span class="cell-muted">Not scored</span>
<% } else { %>
				<span class="risk risk--<%= esc(bottleneck.get("risk_level") == null ? "" : bottleneck.get("risk_level").toLowerCase()) %>"><%= esc(bottleneck.get("risk_level")) %> risk</span>
<% } %>
			</div>
			<p class="detail-desc"><%= course.get("description") == null ? "No description available." : esc(course.get("description")) %></p>
<% } else { %>
			<h1 class="pagehead__title">Course</h1>
<% } %>
		</div>
	</div>
</header>

<main class="layout" id="main">
<% if (loadError != null) { %>
	<div class="detail">
		<div class="banner banner--error" role="alert"><%= esc(loadError) %></div>
	</div>
<% } else { %>
	<div class="detail">
		<dl class="detail-facts">
			<dt>Units</dt>
			<dd><%= esc(course.get("units")) %></dd>

			<dt>Fulfills</dt>
			<dd><%= course.get("ge_area_code") == null ? "None"
				: "Area " + esc(course.get("ge_area_code")) + " - " + esc(course.get("ge_name")) %></dd>

			<dt>Prerequisite(s)</dt>
			<dd>
<% if (groups.isEmpty()) { %>
				None
<% } else { %>
<% for (int g = 0; g < groups.size(); g++) {
	List<Map<String, String>> group = groups.get(g); %>
				<%= g > 0 ? " <em>and</em> " : "" %><%
	for (int i = 0; i < group.size(); i++) {
		Map<String, String> pre = group.get(i); %><%= i > 0 ? " <em>or</em> " : "" %><a class="course-link" href="<%= context %>/course.jsp?id=<%= esc(pre.get("course_id")) %>"><%= esc(pre.get("subject")) %> <%= esc(pre.get("course_number")) %></a><%
	} %>
<% } %>
<% } %>
			</dd>

			<dt>Is a prerequisite for</dt>
			<dd>
<% if (dependents.isEmpty()) { %>
				None
<% } else { %>
<% for (int i = 0; i < dependents.size(); i++) {
	Map<String, String> dep = dependents.get(i); %><%= i > 0 ? ", " : "" %><a class="course-link" href="<%= context %>/course.jsp?id=<%= esc(dep.get("course_id")) %>"><%= esc(dep.get("subject")) %> <%= esc(dep.get("course_number")) %></a><%
} %>
<% } %>
			</dd>
		</dl>

		<form class="detail-actions" method="post"
		      action="<%= context %>/course.jsp?id=<%= esc(course.get("course_id")) %><%= section == null ? "" : "&amp;section=" + esc(section.get("section_id")) %>">
<% if (!signedIn) { %>
			<span class="detail-actions__hint"><a href="<%= context %>/login">Sign in</a> to mark courses completed</span>
<% } else if (!canEdit) { %>
			<span class="detail-actions__hint">Sign in again to mark courses completed</span>
<% } %>
			<label class="check<%= canEdit ? "" : " is-disabled" %>">
				<%-- ticking or unticking saves right away, the button is for browsers without JavaScript --%>
				<input type="checkbox" name="completed" value="1"<%= completed ? " checked" : "" %><%= canEdit ? "" : " disabled" %>
				       onchange="this.form.submit()">
				<span>Completed</span>
			</label>
			<noscript><button class="btn btn--sm" type="submit">Save</button></noscript>
			<%-- TODO: wire to POST /plan/add, same as the catalog button --%>
			<button class="btn btn--sm" type="button" disabled>+ Plan</button>
		</form>
	</div>

	<aside class="rail">
		<div class="rail__stack">
<%@ include file="/WEB-INF/jspf/rail-art.jspf" %>
			<div class="rail__cards">
				<div class="card">
					<span class="card__eyebrow">Enrollment</span>
<% if (section == null) { %>
					<p class="card__body">No section on record for this course.</p>
<% } else { %>
					<div class="card__row"><span>Class capacity</span><span><%= orDash(section.get("seat_capacity")) %></span></div>
					<div class="card__row"><span>Available seats</span><span><%= orDash(section.get("open_seats")) %></span></div>
					<div class="card__row"><span>Waitlist</span><span><%= orDash(section.get("waitlist_count")) %></span></div>
<% } %>
				</div>
			</div>
		</div>
	</aside>
<% } %>
</main>

<script src="<%= context %>/js/header.js" defer></script>
</body>
</html>
