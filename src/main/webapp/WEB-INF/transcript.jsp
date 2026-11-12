<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- My Transcript page, TranscriptServlet forwards here. Lists completed courses, remaining requirements audit, and searches unfinished categories for more. --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Transcript - CourseCraft</title>
	<%@ include file="/WEB-INF/jspf/head.jspf" %>
	<script src="<%= request.getContextPath() %>/js/header.js" defer></script>
	<script src="<%= request.getContextPath() %>/js/transcript.js" defer></script>
</head>
<body>
<c:set var="context" value="${pageContext.request.contextPath}"/>

<a class="skip-link" href="#main">Skip to transcript</a>

<%-- site header: shared top bar plus this page's head, slides away together on scroll (header.js) --%>
<% request.setAttribute("navPage", "transcript"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
	<div class="pagehead tx-import">
		<div class="pagehead__text">
			<h1 class="pagehead__title">Import your transcript</h1>
			<p class="pagehead__meta">
				Upload your MySJSU unofficial transcript, saved from the browser as a web page (.html).
				Upload SJ_SA_CUSTOM.SJ_TR_SS_TSCRPT.html from the "_files" folder.
				Every course that you passed is marked as completed.
			</p>
		</div>
		<div class="tx-import__controls">
			<label class="visually-hidden" for="tx-file">Unofficial transcript file</label>
			<%-- multiple, so the saved page and the inner frame file in its _files folder can go in together --%>
			<input class="tx-import__file" id="tx-file" type="file" accept=".html,.htm,text/html" multiple>
			<p class="tx-import__status" id="tx-import-status" role="status" aria-live="polite"></p>
		</div>
	</div>
</header>

<%-- two halves: transcript on the left, audit and search on the right, stacked on narrow screens --%>
<main class="layout layout--split" id="main">
<c:if test="${not empty savedMessage}">
	<%-- popup, shows for 2 seconds then fades out (app.css), script below removes it after --%>
	<div class="toast" id="saved-toast" role="status"><c:out value="${savedMessage}"/></div>
</c:if>

	<form class="tx-form" id="transcript-form" method="post" action="${context}/transcript">
		<%-- carried through the save so the redirect lands back on the same search --%>
		<input type="hidden" name="q" value="<c:out value='${query}'/>">
		<input type="hidden" name="area" value="<c:out value='${areaKey}'/>">
		<div class="tx-section__head">
			<h2 class="tx-section__title">Transcript</h2>
			<button class="btn btn--primary" type="submit">Save changes</button>
		</div>
<c:if test="${not anyDone}">
		<p class="tx-empty">No completed courses yet. Import your transcript or use the search to add some.</p>
</c:if>
<%-- every category shows, even with nothing completed, so the student sees what is left --%>
<c:forEach var="group" items="${groups}">
		<section class="tx-group">
			<h3 class="tx-group__title"><c:out value="${group.title}"/>
				<span class="tx-group__count">${group.done} of ${group.total}</span></h3>
			<p class="tx-group__note"><c:out value="${group.note}"/></p>
	<c:choose>
		<c:when test="${group.done == 0}">
			<p class="tx-list tx-list--empty">Nothing completed yet.</p>
		</c:when>
		<c:otherwise>
			<ul class="tx-list">
			<c:forEach var="course" items="${group.courses}">
				<c:if test="${course.passed}">
				<li class="tx-row">
					<input type="hidden" name="shown" value="${course.courseId}">
					<input type="checkbox" id="course-${course.courseId}" name="completed" value="${course.courseId}" checked>
					<label class="tx-label" for="course-${course.courseId}">
						<span class="tx-code"><c:out value="${course.code}"/></span>
						<span class="tx-title"><c:out value="${course.title}"/></span>
					</label>
					<span class="tx-units"><c:out value="${course.units}"/> units</span>
					<a class="course-link tx-view" href="${context}/course.jsp?id=${course.courseId}">Details</a>
				</li>
				</c:if>
			</c:forEach>
			</ul>
		</c:otherwise>
	</c:choose>
		</section>
</c:forEach>
		<div class="tx-actions">
			<button class="btn btn--primary" type="submit">Save changes</button>
		</div>
	</form>

	<%-- right column: remaining requirements audit + course search --%>
	<section class="tx-search" aria-labelledby="tx-search-title">
		<h2 class="tx-section__title">Remaining requirements</h2>
<c:choose>
	<c:when test="${empty audit}">
		<p class="tx-empty">Set your major on <a class="course-link" href="${context}/profile">Profile</a> to see your remaining degree requirements.</p>
	</c:when>
	<c:otherwise>
		<p class="tx-group__note"><c:out value="${audit.roadmap.label}"/> &mdash; <a class="course-link" href="${context}/roadmap">View full roadmap</a></p>

		<div class="tx-group">
			<h3 class="tx-group__title">Required courses
				<c:if test="${audit.totalCourses > 0}">
					<span class="tx-group__count">${audit.doneCourses} of ${audit.totalCourses}</span>
				</c:if>
			</h3>
		<c:choose>
			<c:when test="${audit.totalCourses == 0}">
			<p class="tx-list tx-list--empty">No required courses listed for this roadmap.</p>
			</c:when>
			<c:when test="${empty audit.remainingCourses}">
			<p class="tx-list tx-list--empty">All required roadmap courses completed.</p>
			</c:when>
			<c:otherwise>
			<ul class="tx-list">
				<c:forEach var="req" items="${audit.remainingCourses}">
				<li class="tx-row">
					<span class="tx-code"><c:out value="${req.code}"/></span>
					<span class="tx-title"><c:out value="${req.title}"/> (Term ${req.suggestedTerm} &middot; <c:out value="${req.requirementType}"/>)</span>
					<span class="tx-units"><c:out value="${req.units}"/> units</span>
					<a class="course-link tx-view" href="${context}/course.jsp?id=${req.courseId}">Details</a>
				</li>
				</c:forEach>
			</ul>
			</c:otherwise>
		</c:choose>
		</div>

		<c:if test="${audit.totalElectives > 0}">
		<div class="tx-group">
			<h3 class="tx-group__title">Approved major electives
				<span class="tx-group__count">${audit.doneElectives} of ${audit.totalElectives} taken</span></h3>
			<p class="tx-group__note">Roadmap elective options (check catalog for the number of electives your major requires).</p>
		<c:choose>
			<c:when test="${empty audit.remainingElectives}">
			<p class="tx-list tx-list--empty">All listed elective options completed.</p>
			</c:when>
			<c:otherwise>
			<ul class="tx-list">
				<c:forEach var="el" items="${audit.remainingElectives}">
				<li class="tx-row">
					<span class="tx-code"><c:out value="${el.code}"/></span>
					<span class="tx-title"><c:out value="${el.title}"/> (Term ${el.suggestedTerm})</span>
					<span class="tx-units"><c:out value="${el.units}"/> units</span>
					<a class="course-link tx-view" href="${context}/course.jsp?id=${el.courseId}">Details</a>
				</li>
				</c:forEach>
			</ul>
			</c:otherwise>
		</c:choose>
		</div>
		</c:if>

		<div class="tx-group">
			<h3 class="tx-group__title">General education areas
				<c:if test="${audit.totalGe > 0}">
					<span class="tx-group__count">${audit.doneGe} of ${audit.totalGe}</span>
				</c:if>
			</h3>
		<c:choose>
			<c:when test="${audit.totalGe == 0}">
			<p class="tx-list tx-list--empty">No general education requirements listed for this roadmap.</p>
			</c:when>
			<c:when test="${empty audit.remainingGeAreas}">
			<p class="tx-list tx-list--empty">All required GE areas fulfilled.</p>
			</c:when>
			<c:otherwise>
			<ul class="tx-list">
				<c:forEach var="geArea" items="${audit.remainingGeAreas}">
				<li class="tx-row">
					<span class="tx-code">Area <c:out value="${geArea.code}"/></span>
					<span class="tx-title"><c:out value="${geArea.name}"/></span>
					<c:choose>
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
			</c:otherwise>
		</c:choose>
		</div>
	</c:otherwise>
</c:choose>

		<h2 class="tx-section__title" id="tx-search-title">Add completed courses</h2>
		<p class="tx-group__note">Search courses in categories you have not finished, tick the ones you passed, then save.</p>
<c:choose>
	<c:when test="${empty openGroups}">
		<p class="tx-empty">Every category is complete.</p>
	</c:when>
	<c:otherwise>
		<form class="tx-search__form" role="search" method="get" action="${context}/transcript">
			<label class="visually-hidden" for="q">Search courses</label>
			<input class="search-input" id="q" name="q" type="search"
			       placeholder="Search course or title" value="<c:out value='${query}'/>">
			<label class="visually-hidden" for="area">Category</label>
			<select class="search-input tx-search__area" id="area" name="area">
				<option value="">All unfinished categories</option>
			<c:forEach var="group" items="${openGroups}">
				<option value="<c:out value='${group.key}'/>"${group.key == areaKey ? ' selected' : ''}><c:out value="${group.title}"/> (${group.done} of ${group.total})</option>
			</c:forEach>
			</select>
			<button class="btn" type="submit">Search</button>
		<c:if test="${searching}">
			<a class="btn btn--ghost" href="${context}/transcript">Clear</a>
		</c:if>
		</form>
	<c:if test="${searching}">
		<c:choose>
			<c:when test="${empty results}">
		<p class="tx-empty">No unfinished courses match your search.</p>
			</c:when>
			<c:otherwise>
		<p class="tx-group__note">${resultCount} ${resultCount == 1 ? 'course' : 'courses'} found</p>
				<c:forEach var="group" items="${results}">
		<div class="tx-group">
			<h3 class="tx-group__title"><c:out value="${group.title}"/>
				<span class="tx-group__count">${group.done} of ${group.total}</span></h3>
			<ul class="tx-list">
					<c:forEach var="course" items="${group.courses}">
				<li class="tx-row">
					<input type="hidden" name="shown" value="${course.courseId}" form="transcript-form">
					<input type="checkbox" id="add-${course.courseId}" name="completed" value="${course.courseId}" form="transcript-form">
					<label class="tx-label" for="add-${course.courseId}">
						<span class="tx-code"><c:out value="${course.code}"/></span>
						<span class="tx-title"><c:out value="${course.title}"/></span>
					</label>
					<span class="tx-units"><c:out value="${course.units}"/> units</span>
					<a class="course-link tx-view" href="${context}/course.jsp?id=${course.courseId}">Details</a>
				</li>
					</c:forEach>
			</ul>
		</div>
				</c:forEach>
			</c:otherwise>
		</c:choose>
	</c:if>
	</c:otherwise>
</c:choose>
	</section>

	<%-- import posts here: transcript.js adds a shown + completed pair for each passed course it finds --%>
	<form id="import-form" method="post" action="${context}/transcript" hidden>
		<input type="hidden" name="q" value="<c:out value='${query}'/>">
		<input type="hidden" name="area" value="<c:out value='${areaKey}'/>">
		<input type="hidden" name="imported" value="0">
	</form>

	<%-- every catalog course, so the import can match transcript codes to course ids. TranscriptServlet escapes it for <script>. --%>
	<script type="application/json" id="tx-courses">${coursesJson}</script>

	<%-- every major with a roadmap, so the import can match the transcript's plan line; data-current is the student's major --%>
	<script type="application/json" id="tx-majors" data-current="<c:out value='${currentMajorId}'/>">${majorsJson}</script>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>

<c:if test="${not empty savedMessage}">
<script>
	// popup fades out by itself (app.css), then leaves the page
	(function () {
		var toast = document.getElementById("saved-toast");
		toast.addEventListener("animationend", function () { toast.remove(); });
	})();
</script>
</c:if>
</body>
</html>