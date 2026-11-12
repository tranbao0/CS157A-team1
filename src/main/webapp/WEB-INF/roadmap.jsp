<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Roadmap page, RoadmapServlet forwards here --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Roadmap - CourseCraft</title>
	<%@ include file="/WEB-INF/jspf/head.jspf" %>
	<script src="${pageContext.request.contextPath}/js/header.js" defer></script>
</head>
<body>
<c:set var="context" value="${pageContext.request.contextPath}"/>

<a class="skip-link" href="#main">Skip to main content</a>

<%@ include file="/WEB-INF/jspf/road.jspf" %>

<% request.setAttribute("navPage", "roadmap"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main class="layout layout--split" id="main">
<c:choose>
	<c:when test="${empty roadmapData}">
		<div class="detail">
			<h1 class="narrow__title">Roadmap</h1>
			<p class="note">Set your major on <a class="course-link" href="${context}/profile">Profile</a> to view your major roadmap and degree progress.</p>
		</div>
	</c:when>
	<c:otherwise>
		<section class="tx-form" aria-labelledby="roadmap-title">
			<div class="tx-section__head">
				<div>
					<h1 class="tx-section__title" id="roadmap-title"><c:out value="${roadmapData.roadmap.label}"/></h1>
					<p class="tx-group__note">
						<c:choose>
							<c:when test="${roadmapData.totalCourses > 0}">
								${roadmapData.doneCourses} of ${roadmapData.totalCourses} required courses completed
							</c:when>
							<c:otherwise>
								No required courses listed for this roadmap
							</c:otherwise>
						</c:choose>
						<c:if test="${roadmapData.totalElectives > 0}">
							&middot; ${roadmapData.doneElectives} of ${roadmapData.totalElectives} elective options taken
						</c:if>
						<c:if test="${roadmapData.totalGe > 0}">
							&middot; ${roadmapData.doneGe} of ${roadmapData.totalGe} GE areas fulfilled
						</c:if>
					</p>
				</div>
				<a class="btn btn--sm" href="${context}/transcript">Edit transcript</a>
			</div>

			<c:choose>
				<c:when test="${empty roadmapData.terms}">
					<p class="note">No required courses listed for this roadmap.</p>
				</c:when>
				<c:otherwise>
					<c:forEach var="term" items="${roadmapData.terms}">
						<section class="tx-group">
							<h2 class="tx-group__title"><c:out value="${term.label}"/>
								<span class="tx-group__count"><c:out value="${term.unitsText}"/> units</span></h2>
							<ul class="tx-list">
								<c:forEach var="course" items="${term.courses}">
									<li class="tx-row">
										<span class="tx-code"><c:out value="${course.code}"/></span>
										<span class="tx-title"><c:out value="${course.title}"/> (<c:out value="${course.requirementType}"/>)</span>
										<span class="tx-units"><c:out value="${course.units}"/> units</span>
										<c:choose>
											<c:when test="${course.passed}">
												<span class="risk risk--low">Completed</span>
											</c:when>
											<c:otherwise>
												<span class="cell-muted">Remaining</span>
											</c:otherwise>
										</c:choose>
										<a class="course-link tx-view" href="${context}/course.jsp?id=${course.courseId}">Details</a>
									</li>
								</c:forEach>
							</ul>
						</section>
					</c:forEach>
				</c:otherwise>
			</c:choose>
		</section>

		<section class="tx-search" aria-labelledby="roadmap-ge-title">
			<h2 class="tx-section__title" id="roadmap-ge-title">General education areas</h2>
			<c:choose>
				<c:when test="${roadmapData.totalGe == 0}">
					<p class="tx-group__note">No general education requirements listed for this roadmap.</p>
				</c:when>
				<c:otherwise>
					<p class="tx-group__note">${roadmapData.doneGe} of ${roadmapData.totalGe} required GE areas fulfilled</p>
					<div class="tx-group">
						<ul class="tx-list">
							<c:forEach var="geArea" items="${roadmapData.geAreas}">
								<li class="tx-row">
									<span class="tx-code">Area <c:out value="${geArea.code}"/></span>
									<span class="tx-title"><c:out value="${geArea.name}"/></span>
									<c:choose>
										<c:when test="${geArea.fulfilled}">
											<span class="risk risk--low"><c:out value="${geArea.fulfilledByCode}"/></span>
										</c:when>
										<c:when test="${geArea.hasCourses}">
											<a class="course-link tx-view" href="${context}/transcript?area=ge-${geArea.code}">Find courses</a>
										</c:when>
										<c:otherwise>
											<span class="cell-muted">Met with science lab</span>
										</c:otherwise>
									</c:choose>
								</li>
							</c:forEach>
						</ul>
					</div>
				</c:otherwise>
			</c:choose>
		</section>
	</c:otherwise>
</c:choose>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>