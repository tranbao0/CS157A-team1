<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My plan - CourseCraft</title>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <script src="${pageContext.request.contextPath}/js/header.js" defer></script>
    <script src="${pageContext.request.contextPath}/js/plan-summary.js" defer></script>
</head>
<body>
<c:set var="context" value="${pageContext.request.contextPath}"/>
<a class="skip-link" href="#main">Skip to plan</a>
<% request.setAttribute("navPage", "plan"); %>
<header class="site-header"><%@ include file="/WEB-INF/jspf/topbar.jspf" %></header>
<main class="progress-page plan-page" id="main">
    <div class="progress-head"><h1>My plan</h1><a class="btn btn--primary" href="${context}/catalog.jsp">Plan a course</a></div>
    <p class="tx-group__note plan-intro">Your saved courses, grouped by the term you plan to take them.</p>
    <p class="tx-group__note">See the courses you still need on <a href="${context}/transcript#tx-audit-title">My transcript</a>.</p>
    <c:if test="${not empty planMessage}"><div class="banner banner--ok" role="status"><c:out value="${planMessage}"/></div></c:if>
    <c:if test="${not empty planError}"><div class="banner banner--error" role="alert"><c:out value="${planError}"/></div></c:if>
    <c:choose>
        <c:when test="${empty planTerms}"><p class="tx-empty">No courses planned yet. Choose a term in the catalog, then use + Plan to add a course.</p></c:when>
        <c:otherwise>
            <nav class="plan-term-links" aria-label="Saved plan terms"><c:forEach var="term" items="${planTerms}"><a class="chip" href="#plan-term-${term.termId}"><c:out value="${term.label}"/></a></c:forEach></nav>
            <c:forEach var="term" items="${planTerms}">
                <section class="saved-term" id="plan-term-${term.termId}" aria-labelledby="plan-title-${term.termId}" data-plan-term="${term.termId}">
                    <div class="tx-section__head"><h2 class="tx-section__title" id="plan-title-${term.termId}"><c:out value="${term.label}"/></h2><span class="tx-group__count"><c:out value="${term.unitsText}"/> units<c:if test="${term.gradTerm}"> · Expected graduation</c:if></span></div>
                    <c:forEach var="warning" items="${term.warnings}">
                        <details class="plan-warn plan-warn--<c:out value='${warning.kind}'/>" data-warn-key="<c:out value='${warning.key}'/>" open><summary class="plan-warn__summary"><span class="plan-warn__title"><c:out value="${warning.title}"/></span><span class="plan-warn__toggle"><span class="plan-warn__hide">Minimize</span><span class="plan-warn__show">Show</span></span></summary><p class="plan-warn__text"><c:out value="${warning.text}"/></p></details>
                    </c:forEach>
                    <ul class="tx-list">
                        <c:forEach var="course" items="${term.courses}">
                            <li class="tx-row saved-course" data-plan-entry="${course.planEntryId}">
                                <div class="tx-label tx-label--static"><a class="course-link progress-course-link" href="${context}/course.jsp?id=${course.courseId}"><span class="tx-code"><c:out value="${course.code}"/></span><span class="tx-title"><c:out value="${course.title}"/></span></a><span class="tx-meta"><c:out value="${course.units}"/> units<c:if test="${not empty course.riskLevel}"> · <c:out value="${course.riskLevel}"/> risk · sample data</c:if></span><c:if test="${not empty course.petitionTermLabel}"><span class="plan-course__dep">Needs <c:out value="${course.petitionTermLabel}"/> petition, via <c:out value="${course.throughCourses}"/></span></c:if></div>
                                <form method="post" action="${context}/plan/remove"><input type="hidden" name="planEntryId" value="${course.planEntryId}"><button class="btn btn--sm btn--ghost" type="submit">Remove<span class="visually-hidden"> <c:out value="${course.code}"/> from <c:out value="${term.label}"/></span></button></form>
                            </li>
                        </c:forEach>
                    </ul>
                </section>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
</body>
</html>
