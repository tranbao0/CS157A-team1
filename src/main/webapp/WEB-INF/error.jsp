<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- one error page for every status code, web.xml sends errors here --%>
<%-- never shows exception or stack trace, those stay in Tomcat log --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Error - CourseCraft</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700;800&display=swap">
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/app.css">
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/motion.css">
	<script src="${pageContext.request.contextPath}/js/header.js" defer></script>
</head>
<body>

<a class="skip-link" href="#main">Skip to main content</a>

<%@ include file="/WEB-INF/jspf/road.jspf" %>

<% request.setAttribute("navPage", "error"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main class="narrow" id="main">
<c:choose>
	<c:when test="${pageContext.errorData.statusCode == 404}">
	<h1 class="narrow__title">Page not found</h1>
	<p class="note">That address does not exist.</p>
	</c:when>
	<c:when test="${pageContext.errorData.statusCode == 501}">
	<h1 class="narrow__title">Not built yet</h1>
	<p class="note">This part of CourseCraft is still being built.</p>
	</c:when>
	<c:otherwise>
	<h1 class="narrow__title">Something went wrong</h1>
	<p class="note">Try again in a moment.</p>
	</c:otherwise>
</c:choose>
	<p class="note"><a href="${pageContext.request.contextPath}/catalog.jsp">Back to catalog</a></p>
</main>

</body>
</html>
