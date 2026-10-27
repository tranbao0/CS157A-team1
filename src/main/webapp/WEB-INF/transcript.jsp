<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- My courses page, TranscriptServlet forwards here --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>My courses - CourseCraft</title>
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
	<h1 class="narrow__title">My courses</h1>

	<%-- TODO: list completed courses, each row has edit form (POST /transcript/edit) and remove form (POST /transcript/remove) --%>
	<p class="note">Your completed courses will show here.</p>

	<%-- TODO: add form, POST /transcript/add --%>
	<p class="note">A form to add a course will show here.</p>

	<%-- TODO: upload form, POST /transcript/upload with enctype="multipart/form-data", leads to /transcript/review --%>
	<p class="note">Transcript upload will show here.</p>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
