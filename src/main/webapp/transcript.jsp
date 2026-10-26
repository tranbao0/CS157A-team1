<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.sql.Connection, java.sql.PreparedStatement, java.sql.ResultSet, java.sql.ResultSetMetaData, java.sql.SQLException" %>
<%@ page import="java.net.URLEncoder, java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.ArrayList, java.util.Arrays, java.util.HashSet, java.util.LinkedHashMap, java.util.List, java.util.Locale, java.util.Map, java.util.Set" %>
<%@ page import="com.coursecraft.util.Db" %>
<%-- Transcript. Lists the courses a student has completed, and searches unfinished categories for more. TODO: move the queries to a servlet. --%>
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

	private static boolean passed(Map<String, String> course) {
		return "1".equals(course.get("passed"));
	}

	// One category of courses: a GE area, or one of the no-GE groups.
	private static final class Group {
		final String key;
		final String title;
		final String note;
		final List<Map<String, String>> courses;
		int done;

		Group(String key, String title, String note, List<Map<String, String>> courses) {
			this.key = key;
			this.title = title;
			this.note = note;
			this.courses = courses;
			for (Map<String, String> c : courses) {
				if (passed(c)) {
					done++;
				}
			}
		}

		boolean finished() {
			return done == courses.size();
		}
	}

	// True when the search text appears in the course code or title, ignoring case.
	private static boolean matches(Map<String, String> course, String query) {
		if (query.isEmpty()) return true;
		String code = course.get("subject") + " " + course.get("course_number");
		String haystack = (code + " " + code.replace(" ", "") + " " + course.get("title")).toLowerCase(Locale.ROOT);
		return haystack.contains(query.toLowerCase(Locale.ROOT));
	}
%>
<%
	String context = request.getContextPath();
	String email = (String) session.getAttribute("email");

	// Transcripts belong to a signed-in student.
	if (email == null) {
		response.sendRedirect(context + "/login");
		return;
	}

	String loadError = null;
	boolean noStudent = false;
	boolean saved = "1".equals(request.getParameter("saved"));
	String query = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
	String areaKey = request.getParameter("area") == null ? "" : request.getParameter("area");
	List<Map<String, String>> courses = new ArrayList<>();
	List<Map<String, String>> geAreas = new ArrayList<>();

	try (Connection connection = Db.getConnection()) {

		// Student, found by email.
		List<Map<String, String>> who = select(connection,
			"SELECT student_id FROM students WHERE email = ?", email);

		if (who.isEmpty()) {
			noStudent = true;
		} else {
			Integer studentId = Integer.valueOf(who.get(0).get("student_id"));

			// Save. "shown" lists every course on the page, "completed" the ticked ones.
			// Ticked becomes a passed row, unticked removes the row.
			if ("POST".equals(request.getMethod())) {
				String[] shownIds = request.getParameterValues("shown");
				String[] tickedIds = request.getParameterValues("completed");
				Set<String> ticked = new HashSet<>(Arrays.asList(tickedIds == null ? new String[0] : tickedIds));

				// Only real courses are written.
				Set<String> valid = new HashSet<>();
				for (Map<String, String> row : select(connection, "SELECT course_id FROM courses")) {
					valid.add(row.get("course_id"));
				}

				// One transaction, so a failure saves nothing.
				connection.setAutoCommit(false);
				try (PreparedStatement keep = connection.prepareStatement(
						"INSERT INTO completed (student_id, course_id, taken_and_passed) VALUES (?, ?, 1) "
						+ "ON DUPLICATE KEY UPDATE taken_and_passed = 1");
						PreparedStatement drop = connection.prepareStatement(
						"DELETE FROM completed WHERE student_id = ? AND course_id = ?")) {

					if (shownIds != null) {
						for (String id : shownIds) {
							if (!valid.contains(id)) {
								continue;
							}
							PreparedStatement statement = ticked.contains(id) ? keep : drop;
							statement.setInt(1, studentId);
							statement.setInt(2, Integer.parseInt(id));
							statement.addBatch();
						}
					}
					keep.executeBatch();
					drop.executeBatch();
					connection.commit();
				} catch (SQLException e) {
					connection.rollback();
					throw e;
				}

				// Redirect so a refresh does not repeat the POST. Keeps the search the student was on.
				response.sendRedirect(context + "/transcript.jsp?saved=1"
					+ "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
					+ "&area=" + URLEncoder.encode(areaKey, StandardCharsets.UTF_8));
				return;
			}

			// Every course, with whether it is a prerequisite, has prerequisites, and is passed.
			// Numeric part first, so 46A comes before 100W.
			courses = select(connection,
				"SELECT c.course_id, c.subject, c.course_number, c.title, c.units, c.ge_area_code, "
				+ "       EXISTS (SELECT 1 FROM counts_towards ct WHERE ct.course_id = c.course_id) AS is_prereq, "
				+ "       EXISTS (SELECT 1 FROM prerequisite_groups pg WHERE pg.course_id = c.course_id) AS has_prereq, "
				+ "       (SELECT cp.taken_and_passed FROM completed cp "
				+ "        WHERE cp.student_id = ? AND cp.course_id = c.course_id) AS passed "
				+ "FROM courses c "
				+ "ORDER BY c.subject, CAST(c.course_number AS UNSIGNED), c.course_number", studentId);

			geAreas = select(connection, "SELECT code, name FROM ge_areas ORDER BY code");
		}
	} catch (SQLException e) {
		getServletContext().log("Transcript failed", e);
		loadError = "Could not read or save the transcript. Check that MySQL is running.";
		response.setStatus(500);
	}

	// Groups, in order: each GE area that has courses, then the courses with no GE area split by
	// prerequisite, then by having prerequisites, then the rest.
	List<Group> groups = new ArrayList<>();

	for (Map<String, String> area : geAreas) {
		List<Map<String, String>> inArea = new ArrayList<>();
		for (Map<String, String> c : courses) {
			if (area.get("code").equals(c.get("ge_area_code"))) {
				inArea.add(c);
			}
		}
		if (!inArea.isEmpty()) {
			groups.add(new Group("ge-" + area.get("code"),
				"Area " + area.get("code") + " - " + area.get("name"), "General education", inArea));
		}
	}

	List<Map<String, String>> prereqCourses = new ArrayList<>();
	List<Map<String, String>> withPrereqs = new ArrayList<>();
	List<Map<String, String>> otherCourses = new ArrayList<>();
	for (Map<String, String> c : courses) {
		if (c.get("ge_area_code") != null) {
			continue;
		}
		if ("1".equals(c.get("is_prereq"))) {
			prereqCourses.add(c);
		} else if ("1".equals(c.get("has_prereq"))) {
			withPrereqs.add(c);
		} else {
			otherCourses.add(c);
		}
	}
	if (!prereqCourses.isEmpty()) {
		groups.add(new Group("prereq", "Prerequisite courses", "No GE area. Another course requires these.", prereqCourses));
	}
	if (!withPrereqs.isEmpty()) {
		groups.add(new Group("has-prereq", "Courses with prerequisites", "No GE area. Not a prerequisite for anything else.", withPrereqs));
	}
	if (!otherCourses.isEmpty()) {
		groups.add(new Group("other", "Other courses", "No GE area, no prerequisites, and not a prerequisite.", otherCourses));
	}

	// Search runs only over unfinished categories, and only once the student types or picks a category.
	List<Group> openGroups = new ArrayList<>();
	for (Group g : groups) {
		if (!g.finished()) {
			openGroups.add(g);
		}
	}
	boolean searching = !query.isEmpty() || !areaKey.isEmpty();
	List<Group> resultGroups = new ArrayList<>();
	List<List<Map<String, String>>> results = new ArrayList<>();
	int resultCount = 0;
	if (searching) {
		for (Group g : openGroups) {
			if (!areaKey.isEmpty() && !areaKey.equals(g.key)) {
				continue;
			}
			List<Map<String, String>> hits = new ArrayList<>();
			for (Map<String, String> c : g.courses) {
				if (!passed(c) && matches(c, query)) {
					hits.add(c);
				}
			}
			if (!hits.isEmpty()) {
				resultGroups.add(g);
				results.add(hits);
				resultCount += hits.size();
			}
		}
	}

	boolean anyDone = false;
	for (Group g : groups) {
		if (g.done > 0) {
			anyDone = true;
		}
	}
%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Transcript - CourseCraft</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700;800&display=swap">
	<link rel="stylesheet" href="<%= context %>/css/app.css">
	<link rel="stylesheet" href="<%= context %>/css/motion.css">
</head>
<body>

<a class="skip-link" href="#main">Skip to transcript</a>

<%-- site header: shared top bar plus this page's head, slides away together on scroll (header.js) --%>
<% request.setAttribute("navPage", "transcript"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
	<div class="pagehead">
		<div class="pagehead__text">
			<h1 class="pagehead__title">Transcript</h1>
		</div>
<% if (loadError == null && !noStudent) { %>
		<button class="btn btn--primary" type="submit" form="transcript-form">Save changes</button>
<% } %>
	</div>
</header>

<%-- two halves: search on the left, transcript on the right, stacked on narrow screens --%>
<main class="layout layout--split" id="main">
<% if (loadError != null) { %>
	<div class="banner banner--error" role="alert"><%= esc(loadError) %></div>
<% } else if (noStudent) { %>
	<div class="banner banner--warn" role="alert">No student account matches <%= esc(email) %>, so there is no transcript to edit.</div>
<% } else { %>
<% if (saved) { %>
	<%-- popup, shows for 2 seconds then fades out (app.css), script below removes it after --%>
	<div class="toast" id="saved-toast" role="status">Transcript saved.</div>
<% } %>

	<%-- search lives outside the save form (forms cannot nest), its checkboxes join it through form="transcript-form" --%>
	<section class="tx-search" aria-labelledby="tx-search-title">
		<h2 class="tx-section__title" id="tx-search-title">Add completed courses</h2>
		<p class="tx-group__note">Search courses in categories you have not finished, tick the ones you passed, then save.</p>
<% if (openGroups.isEmpty()) { %>
		<p class="tx-empty">Every category is complete.</p>
<% } else { %>
		<form class="tx-search__form" role="search" method="get" action="<%= context %>/transcript.jsp">
			<label class="visually-hidden" for="q">Search courses</label>
			<input class="search-input" id="q" name="q" type="search"
			       placeholder="Search course or title" value="<%= esc(query) %>">
			<label class="visually-hidden" for="area">Category</label>
			<select class="search-input tx-search__area" id="area" name="area">
				<option value="">All unfinished categories</option>
<% for (Group g : openGroups) { %>
				<option value="<%= esc(g.key) %>"<%= g.key.equals(areaKey) ? " selected" : "" %>><%= esc(g.title) %> (<%= g.done %> of <%= g.courses.size() %>)</option>
<% } %>
			</select>
			<button class="btn" type="submit">Search</button>
<% if (searching) { %>
			<a class="btn btn--ghost" href="<%= context %>/transcript.jsp">Clear</a>
<% } %>
		</form>
<% if (searching) { %>
<% if (resultGroups.isEmpty()) { %>
		<p class="tx-empty">No unfinished courses match your search.</p>
<% } else { %>
		<p class="tx-group__note"><%= resultCount %> <%= resultCount == 1 ? "course" : "courses" %> found</p>
<% for (int g = 0; g < resultGroups.size(); g++) {
	Group group = resultGroups.get(g); %>
		<div class="tx-group">
			<h3 class="tx-group__title"><%= esc(group.title) %>
				<span class="tx-group__count"><%= group.done %> of <%= group.courses.size() %></span></h3>
			<ul class="tx-list">
<% for (Map<String, String> c : results.get(g)) {
	String id = esc(c.get("course_id")); %>
				<li class="tx-row">
					<input type="hidden" name="shown" value="<%= id %>" form="transcript-form">
					<input type="checkbox" id="add-<%= id %>" name="completed" value="<%= id %>" form="transcript-form">
					<label class="tx-label" for="add-<%= id %>">
						<span class="tx-code"><%= esc(c.get("subject")) %> <%= esc(c.get("course_number")) %></span>
						<span class="tx-title"><%= esc(c.get("title")) %></span>
					</label>
					<span class="tx-units"><%= esc(c.get("units")) %> units</span>
					<a class="course-link tx-view" href="<%= context %>/course.jsp?id=<%= id %>">Details</a>
				</li>
<% } %>
			</ul>
		</div>
<% } %>
<% } %>
<% } %>
<% } %>
	</section>

	<form class="tx-form" id="transcript-form" method="post" action="<%= context %>/transcript.jsp">
		<%-- carried through the save so the redirect lands back on the same search --%>
		<input type="hidden" name="q" value="<%= esc(query) %>">
		<input type="hidden" name="area" value="<%= esc(areaKey) %>">
		<h2 class="tx-section__title">Transcript</h2>
<% if (!anyDone) { %>
		<p class="tx-empty">No completed courses yet. Use the search to add some.</p>
<% } %>
<%-- every category shows, even with nothing completed, so the student sees what is left --%>
<% for (Group group : groups) { %>
		<section class="tx-group">
			<h3 class="tx-group__title"><%= esc(group.title) %>
				<span class="tx-group__count"><%= group.done %> of <%= group.courses.size() %></span></h3>
			<p class="tx-group__note"><%= esc(group.note) %></p>
<% if (group.done == 0) { %>
			<p class="tx-list tx-list--empty">Nothing completed yet.</p>
<% } else { %>
			<ul class="tx-list">
<% for (Map<String, String> c : group.courses) {
	if (!passed(c)) {
		continue;
	}
	String id = esc(c.get("course_id")); %>
				<li class="tx-row">
					<input type="hidden" name="shown" value="<%= id %>">
					<input type="checkbox" id="course-<%= id %>" name="completed" value="<%= id %>" checked>
					<label class="tx-label" for="course-<%= id %>">
						<span class="tx-code"><%= esc(c.get("subject")) %> <%= esc(c.get("course_number")) %></span>
						<span class="tx-title"><%= esc(c.get("title")) %></span>
					</label>
					<span class="tx-units"><%= esc(c.get("units")) %> units</span>
					<a class="course-link tx-view" href="<%= context %>/course.jsp?id=<%= id %>">Details</a>
				</li>
<% } %>
			</ul>
<% } %>
		</section>
<% } %>
		<div class="tx-actions">
			<button class="btn btn--primary" type="submit">Save changes</button>
		</div>
	</form>
<% } %>
</main>

<script src="<%= context %>/js/header.js" defer></script>
<% if (saved) { %>
<script>
	// drop saved=1 from the address, so a refresh does not show the popup again
	(function () {
		var url = new URL(window.location.href);
		url.searchParams.delete("saved");
		history.replaceState(null, "", url.pathname + url.search + url.hash);
		var toast = document.getElementById("saved-toast");
		toast.addEventListener("animationend", function () { toast.remove(); });
	})();
</script>
<% } %>
</body>
</html>
