<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.sql.Connection, java.sql.PreparedStatement, java.sql.ResultSet, java.sql.SQLException" %>
<%@ page import="java.net.URLEncoder, java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.ArrayList, java.util.List" %>
<%@ page import="com.coursecraft.util.Db" %>
<%@ page import="com.coursecraft.service.PlanService" %>
<%@ page import="com.coursecraft.service.ProfileService" %>
<%@ page import="com.coursecraft.model.StudentProfile" %>
<%@ page import="com.coursecraft.web.RegisterServlet" %>
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

	// signed out has no studentId, 0 matches no plan row since AUTO_INCREMENT IDs start at 1
	int studentId = 0;
	if (signedIn) {
		studentId = (Integer) session.getAttribute("studentId");
	}

	// visitor + Plan links go to sign-in, next brings student back to this exact page and filters
	String nextPath = "/catalog.jsp";
	if (request.getQueryString() != null) {
		nextPath = nextPath + "?" + request.getQueryString();
	}
	String signInHref = context + "/login?next=" + URLEncoder.encode(nextPath, StandardCharsets.UTF_8);

	String query = request.getParameter("q");
	query = query == null ? "" : query.trim();

	boolean openOnly = "1".equals(request.getParameter("open"));

	// Filters are fixed text. User input is bound, never concatenated.
	StringBuilder sql = new StringBuilder(
		"SELECT c.course_id, s.section_id, s.session_id, s.class_number, c.subject, c.course_number, s.section_code, "
		+ "       c.title, c.units, s.mode_of_instruction, s.type, "
		+ "       (SELECT e.open_seats FROM enrollment_snapshots e "
		+ "        WHERE e.section_id = s.section_id "
		+ "        ORDER BY e.captured_at DESC LIMIT 1) AS open_seats, "
		+ "       (SELECT b.risk_level FROM bottleneck_scores b "
		+ "        JOIN terms t ON t.term_id = b.term_id "
		+ "        WHERE b.course_id = c.course_id "
		+ "        ORDER BY t.term_id DESC, b.computed_at DESC LIMIT 1) AS risk_level, "
		+ "       EXISTS (SELECT 1 FROM plan_entries pe "
		+ "               WHERE pe.student_id = ? AND pe.course_id = c.course_id) AS in_plan "
		+ "FROM courses c "
		+ "JOIN sections s ON c.course_id = s.course_id "
		+ "WHERE 1 = 1");

	// list holds Objects, since studentId is number and search text is string
	List<Object> params = new ArrayList<>();
	params.add(studentId);

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
			statement.setObject(i + 1, params.get(i));
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
					result.getString("open_seats"), // null if no snapshot yet
					String.valueOf(result.getInt("course_id")),
					String.valueOf(result.getInt("section_id")),
					result.getString("risk_level"), // null if course is not scored
					String.valueOf(result.getInt("session_id")), // + Plan button sends this with course_id
					String.valueOf(result.getInt("in_plan")) // 1 when course is already in student's plan
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
						<a class="course-link" href="<%= context %>/course.jsp?id=<%= esc(row[8]) %>&amp;section=<%= esc(row[9]) %>">
							<span class="section-code"><%= esc(row[0]) %> (<%= esc(row[1]) %>)</span>
						</a>
						<span class="section-num"><%= esc(row[2]) %></span>
					</td>
					<td class="col-title">
						<a class="course-link" href="<%= context %>/course.jsp?id=<%= esc(row[8]) %>&amp;section=<%= esc(row[9]) %>">
							<span class="course-title"><%= esc(row[3]) %></span>
						</a>
						<span class="course-meta"><%= esc(row[4]) %> units</span>
					</td>
					<td class="col-when cell-muted"><%= esc(row[5]) %></td>
					<td class="col-instructor cell-muted"><%= esc(row[6]) %></td>
					<td class="col-seats">
						<span class="seats<%= full ? " seats--none" : "" %>"><%= esc(row[7]) %></span>
					</td>
					<td class="col-risk">
						<div class="risk-cell">
<% if (row[10] == null) { %>
							<span class="cell-muted">Not scored</span>
<% } else { %>
							<span class="risk risk--<%= esc(row[10].toLowerCase()) %>"><%= esc(row[10]) %></span>
<% } %>
						</div>
					</td>
					<td class="col-action">
<% if (signedIn && "1".equals(row[12])) { %>
						<%-- course is already planned in some term, so every section of it shows In plan --%>
						<button class="btn btn--sm is-added" type="button" disabled>In plan</button>
<% } else if (signedIn) { %>
						<%-- plan-add.js sends course and session of this row to POST /plan/add --%>
						<button class="btn btn--sm js-plan-add" type="button"
						        data-course-id="<%= esc(row[8]) %>" data-session-id="<%= esc(row[11]) %>">+ Plan</button>
<% } else { %>
						<%-- visitor gets working sign-in link instead of disabled button, hidden text says where it goes --%>
						<a class="btn btn--sm" href="<%= esc(signInHref) %>">+ Plan<span class="visually-hidden">, sign in to add <%= esc(row[0]) %></span></a>
<% } %>
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
<%
			// PlanService does SQL, plan-summary.jsp only shows planTerms
			try {
				request.setAttribute("planTerms", new PlanService().summarize(studentId));
			} catch (SQLException e) {
				throw new ServletException("Could not load plan.", e);
			}
%>
		<div class="card">
			<span class="card__eyebrow">Your plan</span>
<%@ include file="/WEB-INF/jspf/plan-road.jspf" %>
			<%-- plan-add.js swaps this div with fresh copy from GET /plan/summary after each add --%>
			<div class="plan-summary" id="plan-summary">
				<jsp:include page="/WEB-INF/plan-summary.jsp"/>
			</div>
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
			<%-- signInHref brings student back to same search and filters after sign-in --%>
			<a class="btn btn--block" href="<%= esc(signInHref) %>">Sign in</a>
		</div>
<% } %>
		<div class="card card--dashed">
			<h2 class="card__title">Upload transcript</h2>
			<p class="card__body">Filters the catalog down to the courses you still need.</p>
<% if (signedIn) { %>
			<%-- upload form lives on My courses page (/transcript) --%>
			<a class="btn btn--block" href="<%= context %>/transcript">Upload transcript</a>
<% } else { %>
			<%-- next brings student to upload page right after sign-in --%>
			<a class="btn btn--block" href="<%= context %>/login?next=%2Ftranscript">Sign in to upload</a>
<% } %>
		</div>
		</div>
		</div>
	</aside>
</main>

<%-- plan-add.js puts error text here, empty until something goes wrong --%>
<p class="plan-toast" id="plan-message" role="status"></p>

<%
	// sign-up leaves flag once, so dialog shows only on first catalog visit after new account
	boolean getStarted = false;
	if (signedIn && session.getAttribute(RegisterServlet.GET_STARTED_FLAG) != null) {
		session.removeAttribute(RegisterServlet.GET_STARTED_FLAG);
		getStarted = true;
		// profile form needs same lists /profile loads, ProfileService does SQL
		ProfileService profileService = new ProfileService();
		try {
			StudentProfile startProfile = profileService.load(studentId);
			request.setAttribute("profile", startProfile);
			request.setAttribute("roadmaps", profileService.listRoadmaps());
			request.setAttribute("terms", profileService.listTerms(startProfile.getGradTermId()));
			request.setAttribute("categories", profileService.listCategories());
		} catch (SQLException e) {
			throw new ServletException("Could not load profile form.", e);
		}
		request.setAttribute("profileReturnTo", "catalog");
	}
%>
<% if (getStarted) { %>
<%-- get-started.js opens dialog as modal, Esc or Skip for now closes it, Profile page has same form later --%>
<dialog class="get-started" id="get-started" aria-labelledby="get-started-title">
	<%-- Skip sits next to title, so student sees way out as soon as dialog opens, even on short screens --%>
	<div class="get-started__head">
		<h2 class="get-started__title" id="get-started-title">Welcome to CourseCraft</h2>
		<%-- method="dialog" closes dialog without sending anything to server --%>
		<form method="dialog" class="get-started__skip">
			<button type="submit" class="btn btn--sm btn--ghost">Skip for now</button>
		</form>
	</div>
	<p class="get-started__lede">Tell us your major, graduation term and SJSU placement. CourseCraft uses them to check prerequisites and build your plan. You can change them any time on Profile.</p>
<%@ include file="/WEB-INF/jspf/profile-form.jspf" %>
</dialog>
<script src="<%= context %>/js/get-started.js" defer></script>
<% } %>

<script src="<%= context %>/js/header.js" defer></script>
<script src="<%= context %>/js/catalog.js" defer></script>
<script src="<%= context %>/js/plan-add.js" defer></script>
<script src="<%= context %>/js/plan-summary.js" defer></script>
</body>
</html>
