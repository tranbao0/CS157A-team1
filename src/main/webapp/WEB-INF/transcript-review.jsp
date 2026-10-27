<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Check your transcript page, TranscriptReviewServlet forwards here --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Check your transcript - CourseCraft</title>
	<%@ include file="/WEB-INF/jspf/head.jspf" %>
	<script src="<%= request.getContextPath() %>/js/header.js" defer></script>
</head>
<body>

<a class="skip-link" href="#main">Skip to main content</a>

<%@ include file="/WEB-INF/jspf/road.jspf" %>

<% request.setAttribute("navPage", "transcript"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main class="narrow" id="main">
	<h1 class="narrow__title">Check your transcript</h1>

	<%-- TODO: parsed major and courses, checkbox on each row, Save posts to /transcript/review --%>
	<p class="note">Courses read from your transcript will show here, so you can check them before saving.</p>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
