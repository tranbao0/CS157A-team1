<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.sql.Connection, java.sql.PreparedStatement, java.sql.ResultSet, java.sql.SQLException" %>
<%@ page import="java.util.ArrayList, java.util.List" %>
<%@ page import="com.coursecraft.util.Db" %>
<%-- Catalog. Browser, Tomcat, MySQL. --%>
<%-- TODO: move query to a servlet --%>
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
%>
<%
	String context = request.getContextPath();
	String email = (String) session.getAttribute("email");
	boolean signedIn = email != null;

	String query = request.getParameter("q");
	query = query == null ? "" : query.trim();

	boolean openOnly = "1".equals(request.getParameter("open"));

	// Filters are fixed text. User input is bound, never concatenated.
	StringBuilder sql = new StringBuilder(
		"SELECT s.class_number, c.subject, c.course_number, s.section_code, "
		+ "       c.title, c.units, s.mode_of_instruction, s.type, "
		+ "       (SELECT e.open_seats FROM enrollment_snapshots e "
		+ "        WHERE e.section_id = s.section_id "
		+ "        ORDER BY e.captured_at DESC LIMIT 1) AS open_seats "
		+ "FROM courses c "
		+ "JOIN sections s ON c.course_id = s.course_id "
		+ "WHERE 1 = 1");

	List<String> params = new ArrayList<>();

	if (!query.isEmpty()) {
		sql.append(" AND (CONCAT(c.subject, ' ', c.course_number) LIKE ?"
				+ " OR c.title LIKE ?)");
		String like = "%" + query + "%";
		params.add(like);
		params.add(like);
	}

	if (openOnly) {
		sql.append(" HAVING open_seats > 0");
	}

	sql.append(" ORDER BY c.subject, c.course_number, s.section_code");
%>
<%
	// runs before HTML, site header shows row count
	List<String[]> rows = new ArrayList<>();
	String loadError = null;

	// Closes on failure too.
	try (Connection connection = Db.getConnection();
			PreparedStatement statement = connection.prepareStatement(sql.toString())) {

		for (int i = 0; i < params.size(); i++) {
			statement.setString(i + 1, params.get(i));
		}

		try (ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				rows.add(new String[] {
					result.getString("subject") + " " + result.getString("course_number"),
					result.getString("section_code"),
					String.valueOf(result.getInt("class_number")),
					result.getString("title"),
					String.valueOf(result.getDouble("units")),
					result.getString("mode_of_instruction"),
					result.getString("type"),
					result.getString("open_seats") // null if no snapshot yet
				});
			}
		}
	} catch (SQLException e) {
		getServletContext().log("Catalog query failed", e);
		loadError = "Could not read the catalog. Check that MySQL is running and that "
				+ "schema.sql and seed.sql have both been loaded.";
	}
%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Course catalog - CourseCraft</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700;800&display=swap">
	<link rel="stylesheet" href="<%= context %>/css/app.css">
	<link rel="stylesheet" href="<%= context %>/css/motion.css">
</head>
<body>

<a class="skip-link" href="#catalog">Skip to catalog</a>

<%-- site header: shared top bar plus this page's head, slides away together on scroll (header.js) --%>
<% request.setAttribute("navPage", "catalog"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
	<div class="pagehead">
		<div class="pagehead__text">
			<h1 class="pagehead__title"><span class="pagehead__term">Fall 2026</span> course catalog</h1>
			<%-- TODO: show scrape date --%>
			<p class="pagehead__meta"><%= rows.size() %> sections &middot; sample data, read from MySQL</p>
		</div>
		<div class="filterbar">
			<%-- TODO: use the term table --%>
			<span class="chip is-on">Fall 2026</span>
			<a class="chip<%= openOnly ? " is-on" : "" %>"
			   href="<%= context %>/catalog.jsp?<%= openOnly ? "" : "open=1&amp;" %>q=<%= esc(query) %>">Open seats only</a>
		</div>
	</div>
</header>

<main class="layout" id="catalog">
	<div class="catalog-wrap">
<% if (loadError != null) { %>
		<div class="banner banner--error" role="alert"><%= esc(loadError) %></div>
<% } else if (rows.isEmpty()) { %>
		<div class="state">
			<h2 class="state__title">No sections match your search</h2>
			<p class="state__body">Try a course code like CS 146, or clear your filters.</p>
		</div>
<% } else { %>
		<table class="catalog">
			<thead>
				<tr>
					<th class="col-section" scope="col">Section</th>
					<th class="col-title" scope="col">Course title</th>
					<th class="col-when" scope="col">Mode</th>
					<th class="col-instructor" scope="col">Type</th>
					<th class="col-seats" scope="col">Seats</th>
					<th class="col-risk" scope="col">Risk</th>
					<th class="col-action" scope="col"><span class="visually-hidden">Add</span></th>
				</tr>
			</thead>
			<tbody>
<%
		for (String[] row : rows) {
			boolean full = "0".equals(row[7]);
%>
				<tr>
					<td class="col-section">
						<span class="section-code"><%= esc(row[0]) %> (<%= esc(row[1]) %>)</span>
						<span class="section-num"><%= esc(row[2]) %></span>
					</td>
					<td class="col-title">
						<span class="course-title"><%= esc(row[3]) %></span>
						<span class="course-meta"><%= esc(row[4]) %> units</span>
					</td>
					<td class="col-when cell-muted"><%= esc(row[5]) %></td>
					<td class="col-instructor cell-muted"><%= esc(row[6]) %></td>
					<td class="col-seats">
						<span class="seats<%= full ? " seats--none" : "" %>"><%= esc(row[7]) %></span>
					</td>
					<td class="col-risk">
						<div class="risk-cell">
							<span class="risk risk--locked">Locked</span>
						</div>
					</td>
					<td class="col-action">
						<button class="btn btn--sm" type="button" disabled>+ Plan</button>
					</td>
				</tr>
<%
		}
%>
			</tbody>
		</table>
<% } %>
	</div>

	<aside class="rail">
		<%-- stack keeps search, cards and art together, so they center as one group in right column --%>
		<div class="rail__stack">
<%@ include file="/WEB-INF/jspf/rail-art.jspf" %>
		<%-- search sits on top of right column so it stays on screen while table scrolls --%>
		<form class="search-form" role="search" method="get" action="<%= context %>/catalog.jsp">
			<label class="visually-hidden" for="q">Search courses</label>
			<input class="search-input" id="q" name="q" type="search"
			       placeholder="Search course or title"
			       value="<%= esc(query) %>">
<% if (openOnly) { %>
			<input type="hidden" name="open" value="1">
<% } %>
			<button class="btn" type="submit">Search</button>
		</form>
		<div class="rail__cards">
<% if (signedIn) { %>
		<div class="card">
			<span class="card__eyebrow">Your plan</span>
<%@ include file="/WEB-INF/jspf/plan-road.jspf" %>
			<h2 class="card__title">Nothing saved yet</h2>
			<p class="card__body">Planning is not built yet. Adding sections comes next.</p>
		</div>
<% } else { %>
		<div class="card">
			<span class="card__eyebrow">Your plan</span>
<%@ include file="/WEB-INF/jspf/plan-road.jspf" %>
			<h2 class="card__title">Nothing saved yet</h2>
			<p class="card__body">
				Sign in to save sections, upload a transcript and get a term-by-term
				plan that respects prerequisites and your unit cap.
			</p>
			<a class="btn btn--primary btn--block" href="<%= context %>/register">Create account</a>
			<a class="btn btn--block" href="<%= context %>/login">Sign in</a>
		</div>
<% } %>
		<div class="card card--dashed">
			<h2 class="card__title">Upload transcript</h2>
			<p class="card__body">Filters the catalog down to the courses you still need.</p>
			<a class="btn btn--block" href="<%= context %>/login">Sign in to upload</a>
		</div>
		</div>
		</div>
	</aside>
</main>

<script src="<%= context %>/js/header.js" defer></script>
<script src="<%= context %>/js/catalog.js" defer></script>
</body>
</html>
