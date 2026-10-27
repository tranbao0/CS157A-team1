<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Profile page, ProfileServlet forwards here --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Profile - CourseCraft</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Figtree:wght@400;500;600;700;800&display=swap">
	<link rel="stylesheet" href="<%= request.getContextPath() %>/css/app.css">
	<link rel="stylesheet" href="<%= request.getContextPath() %>/css/motion.css">
	<script src="<%= request.getContextPath() %>/js/header.js" defer></script>
</head>
<body>

<a class="skip-link" href="#main">Skip to main content</a>

<%@ include file="/WEB-INF/jspf/road.jspf" %>

<% request.setAttribute("navPage", "profile"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main class="narrow" id="main">
	<h1 class="narrow__title">Profile</h1>

	<%-- ProfileServlet sets saved once after redirect from POST, role="status" lets screen readers announce it --%>
	<c:if test="${saved}">
		<div class="banner banner--ok" role="status">Profile saved.</div>
	</c:if>
	<%-- refused form comes back with error, role="alert" reads it out right away --%>
	<c:if test="${not empty error}">
		<div class="banner banner--error" role="alert"><c:out value="${error}"/></div>
	</c:if>

<%@ include file="/WEB-INF/jspf/profile-form.jspf" %>
</main>

</body>
</html>
