<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.sql.Connection, java.sql.PreparedStatement, java.sql.ResultSet, java.sql.SQLException" %>
<%@ page import="java.net.URLEncoder, java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.ArrayList, java.util.List" %>
<%@ page import="com.coursecraft.util.Db" %>
<%@ page import="com.coursecraft.dao.TermDao" %>
<%@ page import="com.coursecraft.model.Term" %>
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

	// builds catalog address with checked term and encoded search text
	private static String catalogHref(String context, int termId, boolean openOnly, String query) {
		String href = context + "/catalog.jsp?term=" + termId;
		if (openOnly) {
			href += "&open=1";
		}
		if (!query.isEmpty()) {
			href += "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
		}
		return href;
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

	String loadError = null;
	// terms not over yet that have active section, in time order, first one is default
	List<Term> terms = new ArrayList<>();
	try (Connection connection = Db.getConnection()) {
		terms = new TermDao().findWithSections(connection);
	} catch (SQLException e) {
		getServletContext().log("Catalog terms query failed", e);
		loadError = "Could not read the catalog. Check that MySQL is running and that "
				+ "schema.sql and seed.sql have both been loaded.";
	}

	// ?term= has to match term from list, anything else gets first term
	String requestedTerm = request.getParameter("term");
	Term chosenTerm = null;
	for (Term term : terms) {
		if (String.valueOf(term.getTermId()).equals(requestedTerm)) {
			chosenTerm = term;
			break;
		}
	}
	if (chosenTerm == null && !terms.isEmpty()) {
		chosenTerm = terms.get(0);
	}
	// no term at all leaves ID 0, page then skips catalog query
	int termId = 0;
	String termLabel = "";
	if (chosenTerm != null) {
		termId = chosenTerm.getTermId();
		termLabel = chosenTerm.getLabel();
	}

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
		+ "               WHERE pe.student_id = ? AND pe.course_id = c.course_id) AS in_plan, "
		+ "       EXISTS (SELECT 1 FROM completed cp "
		+ "               WHERE cp.student_id = ? AND cp.course_id = c.course_id AND cp.taken_and_passed = 1) AS completed "
		+ "FROM courses c "
		+ "JOIN sections s ON c.course_id = s.course_id "
		+ "JOIN sessions se ON se.session_id = s.session_id "
		+ "WHERE se.term_id = ? AND s.status = 'Active'");

	// list holds Objects, since studentId is number and search text is string
	List<Object> params = new ArrayList<>();
	params.add(studentId);
	params.add(studentId);
	params.add(termId);

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

	sql.append(" ORDER BY c.subject, CAST(c.course_number AS UNSIGNED), c.course_number, s.section_code");
%>
<%
	// runs before HTML, site header shows row count
	List<String[]> rows = new ArrayList<>();

	if (termId != 0) {
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
						String.valueOf(result.getInt("in_plan")), // 1 when course is already in student's plan
						String.valueOf(result.getInt("completed")) // 1 when student passed course
					});
				}
			}
		} catch (SQLException e) {
			getServletContext().log("Catalog query failed", e);
			if (loadError == null) {
				loadError = "Could not read the catalog. Check that MySQL is running and that "
						+ "schema.sql and seed.sql have both been loaded.";
			}
		}
	}
%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Course catalog - CourseCraft</title>
	<%@ include file="/WEB-INF/jspf/head.jspf" %>
</head>
<body>

<a class="skip-link" href="#catalog">Skip to catalog</a>

<%-- site header: shared top bar plus this page's head, slides away together on scroll (header.js) --%>
<% request.setAttribute("navPage", "catalog"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
	<div class="pagehead">
		<div class="pagehead__text">
			<h1 class="pagehead__title"><% if (termId != 0) { %><span class="pagehead__term"><%= esc(termLabel) %></span> course catalog<% } else { %>Course catalog<% } %></h1>
			<%-- TODO: show scrape date --%>
			<p class="pagehead__meta"><%= rows.size() %> sections &middot; sample data</p>
		</div>
		<div class="filterbar">
<% if (!terms.isEmpty()) { %>
			<nav class="filterbar__terms" aria-label="Term">
<% for (Term term : terms) { %>
				<a class="chip<% if (term.getTermId() == termId) { %> is-on<% } %>"
				   href="<%= esc(catalogHref(context, term.getTermId(), openOnly, query)) %>"<% if (term.getTermId() == termId) { %> aria-current="true"<% } %>><%= esc(term.getLabel()) %></a>
<% } %>
			</nav>
<% } %>
<% if (termId != 0) { %>
			<a class="chip<% if (openOnly) { %> is-on<% } %>"
			   href="<%= esc(catalogHref(context, termId, !openOnly, query)) %>"<% if (openOnly) { %> aria-current="true"<% } %>>Open seats only</a>
<% } %>
		</div>
	</div>
</header>

<main class="layout" id="catalog">
	<div class="catalog-wrap">
<% if (loadError != null) { %>
		<div class="banner banner--error" role="alert"><%= esc(loadError) %></div>
<% } else if (terms.isEmpty()) { %>
		<div class="state">
			<h2 class="state__title">No sections listed yet</h2>
			<p class="state__body">No term has a class schedule right now.</p>
		</div>
<% } else if (rows.isEmpty()) { %>
		<div class="state">
			<h2 class="state__title">No sections match your search</h2>
			<p class="state__body">Try a course code like CS 146, or clear your filters.</p>
		</div>
<% } else { %>
		<table role="table" class="catalog">
			<thead role="rowgroup">
				<tr role="row">
					<th role="columnheader" class="col-section" scope="col">Section</th>
					<th role="columnheader" class="col-title" scope="col">Course title</th>
					<th role="columnheader" class="col-when" scope="col">Mode</th>
					<th role="columnheader" class="col-instructor" scope="col">Type</th>
					<th role="columnheader" class="col-seats" scope="col">Seats</th>
					<th role="columnheader" class="col-risk" scope="col">Risk</th>
					<th role="columnheader" class="col-action" scope="col"><span class="visually-hidden">Add</span></th>
				</tr>
			</thead>
			<tbody role="rowgroup">
<%
		for (String[] row : rows) {
			boolean full = "0".equals(row[7]);
%>
				<tr role="row">
					<td role="cell" class="col-section">
						<a class="course-link" href="<%= context %>/course.jsp?id=<%= esc(row[8]) %>&amp;section=<%= esc(row[9]) %>">
							<span class="section-code"><%= esc(row[0]) %> (<%= esc(row[1]) %>)</span>
						</a>
						<span class="section-num"><%= esc(row[2]) %></span>
					</td>
					<td role="cell" class="col-title">
						<a class="course-link" href="<%= context %>/course.jsp?id=<%= esc(row[8]) %>&amp;section=<%= esc(row[9]) %>">
							<span class="course-title"><%= esc(row[3]) %></span>
						</a>
						<span class="course-meta"><%= esc(row[4]) %> units</span>
					</td>
					<td role="cell" class="col-when cell-muted"><%= esc(row[5]) %></td>
					<td role="cell" class="col-instructor cell-muted"><%= esc(row[6]) %></td>
					<td role="cell" class="col-seats">
						<span class="seats<%= full ? " seats--none" : "" %>"><%= esc(row[7]) %></span>
					</td>
					<td role="cell" class="col-risk">
						<div class="risk-cell">
<% if (row[10] == null) { %>
							<span class="cell-muted">Not scored</span>
<% } else { %>
							<span class="risk risk--<%= esc(row[10].toLowerCase()) %>"><%= esc(row[10]) %> risk</span>
<% } %>
						</div>
					</td>
					<td role="cell" class="col-action">
<% if (signedIn && "1".equals(row[13])) { %>
						<%-- passed course shows completed for every section --%>
						<button class="btn btn--sm is-added" type="button" disabled>Completed</button>
<% } else if (signedIn && "1".equals(row[12])) { %>
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
<% if (termId != 0) { %>
			<input type="hidden" name="term" value="<%= termId %>">
<% } %>
<% if (openOnly) { %>
			<input type="hidden" name="open" value="1">
<% } %>
			<button class="btn" type="submit">Search</button>
		</form>
		<div class="rail__cards">
<% if (signedIn) { %>
<%
			// plan summary loads only while catalog data is available
			boolean planLoaded = false;
			if (loadError == null) {
				try {
					request.setAttribute("planTerms", new PlanService().summarize(studentId));
					planLoaded = true;
				} catch (SQLException e) {
					getServletContext().log("Could not load plan.", e);
				}
			}
%>
		<div class="card">
			<h2 class="card__title">Your plan</h2>
<%@ include file="/WEB-INF/jspf/plan-road.jspf" %>
			<%-- plan-add.js swaps this div with fresh copy from GET /plan/summary after each add --%>
			<div class="plan-summary" id="plan-summary">
<% if (planLoaded) { %>
				<jsp:include page="/WEB-INF/plan-summary.jsp"/>
<% } else { %>
				<p class="card__body">Could not load your plan.</p>
<% } %>
			</div>
		</div>
<% } else { %>
		<div class="card">
			<h2 class="card__title">Your plan</h2>
<%@ include file="/WEB-INF/jspf/plan-road.jspf" %>
			<h3 class="card__subtitle">Nothing saved yet</h3>
			<p class="card__body">Sign in to save courses to a term-by-term plan. CourseCraft checks prerequisites and SJSU unit limits each time you add a course.</p>
			<a class="btn btn--primary btn--block" href="<%= context %>/register">Create account</a>
			<%-- signInHref brings student back to same search and filters after sign-in --%>
			<a class="btn btn--block" href="<%= esc(signInHref) %>">Sign in</a>
		</div>
<% } %>
		<div class="card card--dashed">
			<h2 class="card__title">Upload transcript</h2>
			<p class="card__body">Marks the courses you passed as completed, so prerequisite checks know what you have done.</p>
<% if (signedIn) { %>
			<a class="btn btn--block" href="<%= context %>/transcript.jsp">Upload transcript</a>
<% } else { %>
			<%-- next sends the student on to the transcript page once they sign in --%>
			<a class="btn btn--block" href="<%= context %>/login?next=/transcript.jsp">Sign in to upload</a>
<% } %>
		</div>
		</div>
		</div>
	</aside>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
<%@ include file="/WEB-INF/jspf/toast.jspf" %>


<%
	// sign-up leaves flag once, so dialog shows only on first catalog visit after new account
	boolean getStarted = false;
	// with database down flag stays, so dialog shows on next catalog visit that works
	if (signedIn && loadError == null && session.getAttribute(RegisterServlet.GET_STARTED_FLAG) != null) {
		// profile form needs same lists /profile loads, ProfileService does SQL
		ProfileService profileService = new ProfileService();
		try {
			StudentProfile startProfile = profileService.load(studentId);
			request.setAttribute("profile", startProfile);
			request.setAttribute("roadmaps", profileService.listRoadmaps());
			request.setAttribute("terms", profileService.listTerms(startProfile.getGradTermId()));
			request.setAttribute("categories", profileService.listCategories());
			// every list loaded, so dialog shows now and flag is used up
			session.removeAttribute(RegisterServlet.GET_STARTED_FLAG);
			getStarted = true;
		} catch (SQLException e) {
			getServletContext().log("Could not load profile form.", e);
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

<script src="<%= context %>/js/toast.js" defer></script>
<script src="<%= context %>/js/header.js" defer></script>
<script src="<%= context %>/js/plan-add.js" defer></script>
<script src="<%= context %>/js/plan-summary.js" defer></script>
</body>
</html>
