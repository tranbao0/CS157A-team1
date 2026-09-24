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
		+ "       c.title, c.units, s.days, s.start_time, s.end_time, "
		+ "       s.instructor, s.open_seats "
		+ "FROM course c "
		+ "JOIN section s ON c.course_id = s.course_id "
		+ "WHERE 1 = 1");

	List<String> params = new ArrayList<>();

	if (!query.isEmpty()) {
		sql.append(" AND (CONCAT(c.subject, ' ', c.course_number) LIKE ?"
				+ " OR c.title LIKE ? OR s.instructor LIKE ?)");
		String like = "%" + query + "%";
		params.add(like);
		params.add(like);
		params.add(like);
	}

	if (openOnly) {
		sql.append(" AND s.open_seats > 0");
	}

	sql.append(" ORDER BY c.subject, c.course_number, s.section_code");
%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Course catalog - CourseCraft</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap">
	<link rel="stylesheet" href="<%= context %>/css/app.css">
</head>
<body>

<a class="skip-link" href="#catalog">Skip to catalog</a>

<header class="topbar">
	<a class="topbar__brand" href="<%= context %>/">CourseCraft</a>
	<nav class="topbar__nav" aria-label="Main">
		<a class="nav-link is-active" aria-current="page" href="<%= context %>/">Catalog</a>
		<%-- TODO: no pages yet --%>
		<span class="nav-link is-disabled" aria-disabled="true">Roadmap</span>
		<span class="nav-link is-disabled" aria-disabled="true">My plan</span>
	</nav>
	<div class="topbar__actions">
<% if (signedIn) { %>
		<span class="topbar__user">Signed in as <%= esc(email) %></span>
		<a class="btn btn--ghost" href="<%= context %>/logout.jsp">Sign out</a>
<% } else { %>
		<a class="btn btn--ghost" href="<%= context %>/login.jsp">Sign in</a>
		<%-- TODO: no register page --%>
		<a class="btn btn--primary" href="<%= context %>/login.jsp">Create account</a>
<% } %>
	</div>
</header>

<%
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
				// Trim seconds.
				String meeting = result.getString("days") + " "
						+ result.getString("start_time").substring(0, 5) + "-"
						+ result.getString("end_time").substring(0, 5);

				rows.add(new String[] {
					result.getString("subject") + " " + result.getString("course_number"),
					result.getString("section_code"),
					String.valueOf(result.getInt("class_number")),
					result.getString("title"),
					String.valueOf(result.getDouble("units")),
					meeting,
					result.getString("instructor"),
					String.valueOf(result.getInt("open_seats"))
				});
			}
		}
	} catch (SQLException e) {
		getServletContext().log("Catalog query failed", e);
		loadError = "Could not read the catalog. Check that MySQL is running and that "
				+ "schema.sql and seed.sql have both been loaded.";
	}
%>

<div class="pagehead">
	<div class="pagehead__text">
		<h1 class="pagehead__title">Fall 2026 course catalog</h1>
		<%-- TODO: show scrape date --%>
		<p class="pagehead__meta"><%= rows.size() %> sections &middot; sample data, read from MySQL</p>
	</div>
	<form class="pagehead__tools" method="get" action="<%= context %>/">
		<label class="visually-hidden" for="q">Search courses</label>
		<input class="search-input" id="q" name="q" type="search"
		       placeholder="Search course, title, or instructor"
		       value="<%= esc(query) %>">
<% if (openOnly) { %>
		<input type="hidden" name="open" value="1">
<% } %>
		<button class="btn" type="submit">Search</button>
	</form>
</div>

<div class="filterbar">
	<%-- TODO: use the term table --%>
	<span class="chip is-on">Fall 2026</span>
	<a class="chip<%= openOnly ? " is-on" : "" %>"
	   href="<%= context %>/?<%= openOnly ? "" : "open=1&amp;" %>q=<%= esc(query) %>">Open seats only</a>
</div>

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
					<th class="col-when" scope="col">Days &amp; times</th>
					<th class="col-instructor" scope="col">Instructor</th>
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
<% if (signedIn) { %>
		<div class="card">
			<span class="card__eyebrow">Your plan</span>
			<h2 class="card__title">Nothing saved yet</h2>
			<p class="card__body">Planning is not built yet. Adding sections comes next.</p>
		</div>
<% } else { %>
		<div class="card">
			<span class="card__eyebrow">Your plan</span>
			<h2 class="card__title">Nothing saved yet</h2>
			<p class="card__body">
				Sign in to save sections, upload a transcript and get a term-by-term
				plan that respects prerequisites and your unit cap.
			</p>
			<a class="btn btn--primary btn--block" href="<%= context %>/login.jsp">Create account</a>
			<a class="btn btn--block" href="<%= context %>/login.jsp">Sign in</a>
		</div>
<% } %>
		<div class="card card--dashed">
			<h2 class="card__title">Upload transcript</h2>
			<p class="card__body">Filters the catalog down to the courses you still need.</p>
			<a class="btn btn--block" href="<%= context %>/login.jsp">Sign in to upload</a>
		</div>
	</aside>
</main>

<script src="<%= context %>/js/catalog.js" defer></script>
</body>
</html>
