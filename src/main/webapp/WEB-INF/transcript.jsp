<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="cc" tagdir="/WEB-INF/tags" %>
<%-- My Transcript page, TranscriptServlet forwards here. Lists completed courses, remaining requirements audit, and searches courses not passed yet. --%>
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

<%-- keep transcript left, stack audit and search right, stack both columns on narrow screens --%>
<main class="layout layout--split" id="main">
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
			<%-- statusText: GE area says "Met by MATH 30", "Met by W-I placement" or "Not met yet", other categories "2 of 4" --%>
			<h3 class="tx-group__title"><c:out value="${group.title}"/>
				<span class="tx-group__count"><c:out value="${group.statusText}"/></span></h3>
			<p class="tx-group__note"><c:out value="${group.note}"/></p>
	<c:choose>
		<c:when test="${group.done == 0 and not empty group.metByPlacement}">
			<p class="tx-list tx-list--empty">No course needed, your <c:out value="${group.metByPlacement}"/> placement meets this area.</p>
		</c:when>
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

	<%-- group audit and course search in right column --%>
	<div class="tx-side">

	<section class="tx-search" aria-labelledby="tx-search-title">
		<h2 class="tx-section__title" id="tx-search-title">Add completed courses</h2>
		<p class="tx-group__note">Search courses you have not marked as passed, tick the ones you passed, then save.</p>
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
				<option value="">All categories</option>
			<c:forEach var="group" items="${openGroups}">
				<option value="<c:out value='${group.key}'/>"${group.key == areaKey ? ' selected' : ''}><c:out value="${group.title}"/> (<c:out value="${group.statusText}"/>)</option>
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
		<p class="tx-empty">No courses left to add match your search.</p>
			</c:when>
			<c:otherwise>
		<p class="tx-group__note">${resultCount} ${resultCount == 1 ? 'course' : 'courses'} found</p>
				<c:forEach var="group" items="${results}">
		<div class="tx-group">
			<h3 class="tx-group__title"><c:out value="${group.title}"/>
				<span class="tx-group__count"><c:out value="${group.statusText}"/></span></h3>
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

	<section class="tx-audit" aria-labelledby="tx-audit-title">
		<h2 class="tx-section__title" id="tx-audit-title">4-Year Roadmap</h2>
<c:choose>
	<c:when test="${empty audit}">
		<p class="tx-empty">Set your major on <a class="course-link" href="${context}/profile">Profile</a> to see your remaining degree requirements.</p>
	</c:when>
	<c:otherwise>
		<p class="tx-group__note">
			<c:out value="${audit.roadmap.label}"/>
        </p>
        <c:set var="plannedRequired" value="0"/>
        <c:set var="plannedOptions" value="0"/>
        <c:forEach var="term" items="${audit.terms}">
            <c:forEach var="req" items="${term.courses}">
                <c:if test="${not req.passed and not empty req.plannedTermLabel}">
                    <c:choose>
                        <c:when test="${req.requirementType == 'Elective'}"><c:set var="plannedOptions" value="${plannedOptions + 1}"/></c:when>
                        <c:otherwise><c:set var="plannedRequired" value="${plannedRequired + 1}"/></c:otherwise>
                    </c:choose>
                </c:if>
            </c:forEach>
        </c:forEach>
        <c:if test="${audit.totalCourses > 0}">
            <div class="rm-progress">
                <p class="rm-progress__text"><strong>${audit.doneCourses} of ${audit.totalCourses}</strong> required courses completed, <strong>${plannedRequired}</strong> planned</p>
                <div class="rm-bar" role="img" aria-label="${audit.doneCourses} of ${audit.totalCourses} required courses completed, ${plannedRequired} planned">
                    <span style="width: ${100 * audit.doneCourses / audit.totalCourses}%"></span>
                    <span class="rm-bar__plan" style="width: ${100 * plannedRequired / audit.totalCourses}%"></span>
                </div>
            </div>
        </c:if>
        <c:if test="${audit.totalElectives > 0}">
            <p class="tx-group__note">${audit.doneElectives} of ${audit.totalElectives} elective options taken · ${plannedOptions} planned. Elective options are choices, not extra required courses.</p>
        </c:if>

		<c:choose>
			<c:when test="${empty audit.terms}">
				<p class="tx-list tx-list--empty">No 4-Year Roadmap courses are on file for this major yet.</p>
			</c:when>
			<c:otherwise>
                <h3 class="tx-group__title">Required courses and elective options by term</h3>
                <c:forEach var="term" items="${audit.terms}">
                    <c:set var="termDone" value="0"/>
                    <c:forEach var="req" items="${term.courses}">
                        <c:if test="${req.passed}"><c:set var="termDone" value="${termDone + 1}"/></c:if>
                    </c:forEach>
                    <c:set var="termLabel" value="${term.label}"/>
                    <c:if test="${term.termNumber > 0}">
                        <c:set var="yearNumber" value="${(term.termNumber + 1 - (term.termNumber + 1) % 2) / 2}"/>
                        <c:set var="termLabel">Year ${fn:substringBefore(yearNumber, '.')} · ${term.termNumber % 2 == 1 ? 'Fall' : 'Spring'}</c:set>
                    </c:if>
                    <details class="tx-group rm-term ${termDone == fn:length(term.courses) ? 'is-done' : ''}" ${termDone == fn:length(term.courses) ? '' : 'open'}>
                        <summary class="tx-group__title">
                            <span><c:out value="${termLabel}"/></span>
                            <span class="tx-group__count"><c:if test="${termDone == fn:length(term.courses)}">✓ All </c:if>${termDone}<c:if test="${termDone != fn:length(term.courses)}"> of ${fn:length(term.courses)}</c:if> completed · <c:out value="${term.unitsText}"/> required units</span>
                        </summary>
                        <ul class="tx-list">
                            <c:forEach var="req" items="${term.courses}">
                                <cc:auditCourseRow course="${req}" meta="${req.requirementType}"/>
                            </c:forEach>
                        </ul>
                    </details>
                </c:forEach>
			</c:otherwise>
		</c:choose>

		<div class="tx-group">
			<h3 class="tx-group__title">General education areas
				<c:if test="${audit.totalGe > 0}">
					<span class="tx-group__count">${audit.doneGe} of ${audit.totalGe} met</span>
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
			<div class="tx-list tx-fold">
			<ul class="tx-list">
				<c:forEach var="geArea" items="${audit.remainingGeAreas}" begin="0" end="4">
				<cc:auditGeRow area="${geArea}"/>
				</c:forEach>
			</ul>
			<c:if test="${fn:length(audit.remainingGeAreas) > 5}">
			<details class="tx-more">
				<summary><span class="tx-more__closed">Show all ${fn:length(audit.remainingGeAreas)}</span><span class="tx-more__open">Show fewer</span></summary>
				<ul class="tx-list">
				<c:forEach var="geArea" items="${audit.remainingGeAreas}" begin="5">
				<cc:auditGeRow area="${geArea}"/>
				</c:forEach>
				</ul>
			</details>
			</c:if>
			</div>
			</c:otherwise>
		</c:choose>
		</div>
	</c:otherwise>
</c:choose>

	</section>

	</div>

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
<c:set var="toastText" value="${savedMessage}" scope="request"/>
<%@ include file="/WEB-INF/jspf/toast.jspf" %>
<script src="${context}/js/toast.js" defer></script>
</body>
</html>