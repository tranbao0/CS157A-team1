<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Roadmap page, RoadmapServlet forwards here --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Roadmap - CourseCraft</title>
	<%@ include file="/WEB-INF/jspf/head.jspf" %>
	<script src="<%= request.getContextPath() %>/js/header.js" defer></script>
</head>
<body>

<a class="skip-link" href="#main">Skip to main content</a>

<%@ include file="/WEB-INF/jspf/road.jspf" %>

<% request.setAttribute("navPage", "roadmap"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main class="narrow" id="main">
	<h1 class="narrow__title">Roadmap</h1>

	<%-- TODO: required courses in suggested term order, then GE areas, each marked done or remaining --%>
	<p class="note">Courses your major needs will show here.</p>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
