<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- My plan page, PlanPage forwards here for PlanServlet and PlanRemoveServlet --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>My plan - CourseCraft</title>
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

<% request.setAttribute("navPage", "plan"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main class="narrow narrow--wide" id="main">
	<h1 class="narrow__title">My plan</h1>

	<p class="note">Every term you planned, oldest first, with unit totals and SJSU unit warnings.</p>

	<%-- success message shows once after remove, role="status" lets screen readers announce it --%>
	<c:if test="${not empty planMessage}">
		<div class="banner banner--ok" role="status"><c:out value="${planMessage}"/></div>
	</c:if>
	<%-- refused remove (404, 409) explains why, role="alert" reads it out right away --%>
	<c:if test="${not empty planError}">
		<div class="banner banner--error" role="alert"><c:out value="${planError}"/></div>
	</c:if>

	<%-- page includes same fragment as plan card in catalog rail, planView "page" from PlanServlet picks full-page look --%>
	<div class="plan-summary plan-summary--page" id="plan-summary">
		<jsp:include page="/WEB-INF/plan-summary.jsp"/>
	</div>
</main>

<%-- closes warnings student minimized before, in rail or here, since both use same keys --%>
<script src="<%= request.getContextPath() %>/js/plan-summary.js" defer></script>

</body>
</html>
