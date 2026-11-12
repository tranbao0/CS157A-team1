<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="cc" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My progress - CourseCraft</title>
    <%@ include file="/WEB-INF/jspf/head.jspf" %>
    <script src="${pageContext.request.contextPath}/js/header.js" defer></script>
    <script src="${pageContext.request.contextPath}/js/transcript.js" defer></script>
    <script src="${pageContext.request.contextPath}/js/progress.js" defer></script>
    <script src="${pageContext.request.contextPath}/js/plan-summary.js" defer></script>
</head>
<body>
<c:set var="context" value="${pageContext.request.contextPath}"/>
<a class="skip-link" href="#main">Skip to progress</a>
<% request.setAttribute("navPage", "progress"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>
<main class="progress-page" id="main">
    <div class="progress-head">
        <h1>My progress</h1>
        <label class="btn btn--primary progress-upload" for="tx-file">Upload transcript
            <input class="visually-hidden" id="tx-file" type="file" accept=".html,.htm,text/html" multiple>
        </label>
    </div>
    <details class="progress-help">
        <summary>How to upload your transcript</summary>
        <p>Save your MySJSU unofficial transcript as a web page (.html). Upload SJ_SA_CUSTOM.SJ_TR_SS_TSCRPT.html from its "_files" folder. Every course you passed is marked as completed.</p>
    </details>
    <p class="tx-import__status" id="tx-import-status" role="status" aria-live="polite"></p>
    <c:if test="${not empty planMessage}"><div class="banner banner--ok" role="status"><c:out value="${planMessage}"/></div></c:if>
    <c:if test="${not empty planError}"><div class="banner banner--error" role="alert"><c:out value="${planError}"/></div></c:if>
    <form id="transcript-form" method="post" action="${context}/transcript">
        <input type="hidden" name="q" value="<c:out value='${query}'/>">
        <input type="hidden" name="area" value="<c:out value='${areaKey}'/>">
    </form>
    <div class="progress-tools">
        <details class="progress-terms">
            <summary>Planned terms <span class="tx-group__count">${planCount} ${planCount == 1 ? 'course' : 'courses'}</span></summary>
            <c:if test="${empty planTerms}"><p class="tx-empty">No courses planned yet. <a href="${context}/catalog.jsp">Browse the catalog</a> to add a course.</p></c:if>
            <c:forEach var="term" items="${planTerms}">
                <section class="progress-plan-term">
                    <h3><c:out value="${term.label}"/> · <c:out value="${term.unitsText}"/> units<c:if test="${term.gradTerm}"> · Expected graduation</c:if></h3>
                    <c:forEach var="warning" items="${term.warnings}">
                        <details class="plan-warn plan-warn--<c:out value='${warning.kind}'/>" data-warn-key="<c:out value='${warning.key}'/>" open>
                            <summary class="plan-warn__summary"><span class="plan-warn__title"><c:out value="${warning.title}"/></span><span class="plan-warn__toggle"><span class="plan-warn__hide">Minimize</span><span class="plan-warn__show">Show</span></span></summary>
                            <p class="plan-warn__text"><c:out value="${warning.text}"/></p>
                        </details>
                    </c:forEach>
                </section>
            </c:forEach>
        </details>
        <details class="tx-search" id="completion-search" ${searching ? 'open' : ''}>
            <summary id="tx-search-title">Add completed courses</summary>
            <p class="tx-group__note">Search for courses you passed. Tick each completed course, then save changes.</p>
            <form class="tx-search__form" role="search" method="get" action="${context}/progress#tx-search-title">
                <label class="visually-hidden" for="q">Search courses</label>
                <input class="search-input" id="q" name="q" type="search" placeholder="Search course or title" value="<c:out value='${query}'/>">
                <label class="visually-hidden" for="area">Category</label>
                <select class="search-input tx-search__area" id="area" name="area">
                    <option value="">All categories</option>
                    <c:forEach var="group" items="${openGroups}"><option value="<c:out value='${group.key}'/>"${group.key == areaKey ? ' selected' : ''}><c:out value="${group.title}"/></option></c:forEach>
                </select>
                <button class="btn" type="submit">Search</button>
                <c:if test="${searching}"><a class="btn btn--ghost" href="${context}/progress">Clear</a></c:if>
            </form>
            <c:if test="${searching}">
                <p class="tx-group__note">${resultCount} ${resultCount == 1 ? 'course' : 'courses'} found</p>
                <c:if test="${empty results}"><p class="tx-empty">No courses left to add match your search.</p></c:if>
                <ul class="tx-list">
                    <c:forEach var="group" items="${results}">
                        <c:forEach var="course" items="${group.courses}">
                            <c:choose>
                                <c:when test="${progressCourseIds.contains(course.courseId)}">
                                    <li class="tx-row progress-result"><a class="course-link" href="#progress-course-${course.courseId}" data-edit-course="${course.courseId}"><c:out value="${course.code}"/> · <c:out value="${course.title}"/></a><span class="tx-meta">Edit in course row</span></li>
                                </c:when>
                                <c:otherwise><cc:auditCourseRow course="${course}" meta="${group.title}" search="true"/></c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </c:forEach>
                </ul>
            </c:if>
        </details>
    </div>
    <details class="completed-courses" id="completed-courses">
        <summary>Completed courses</summary>
        <cc:completedCourses courses="${completedCourses}"/>
    </details>
    <div class="progress-edit-bar">
        <button class="btn btn--ghost" type="button" id="progress-edit" aria-pressed="false" hidden>Edit completed courses</button>
        <p class="tx-group__note" id="progress-edit-help">Tick the courses you passed. Clear a tick to remove a completion.</p>
        <button class="btn" type="submit" form="transcript-form" id="progress-save">Save changes</button>
    </div>
	<section class="tx-audit" aria-labelledby="tx-audit-title">
		<h2 class="tx-section__title" id="tx-audit-title">Degree roadmap</h2>
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
                <h3 class="tx-group__title">Suggested course sequence</h3>
                <p class="tx-group__note">Year and season show the suggested sequence. Your planned term appears on each saved course.</p>
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

		<div class="tx-group progress-ge">
			<h3 class="tx-group__title">General education areas
				<c:if test="${audit.totalGe > 0}">
					<span class="tx-group__count">${audit.doneGe} of ${audit.totalGe} met</span>
				</c:if>
			</h3>
		<c:choose>
			<c:when test="${audit.totalGe == 0}">
			<p class="tx-list tx-list--empty">No general education requirements listed for this roadmap.</p>
			</c:when>
			<c:otherwise>
            <ul class="tx-list">
                <c:forEach var="geArea" items="${audit.geAreas}">
                    <cc:auditGeRow area="${geArea}"/>
                </c:forEach>
            </ul>
			</c:otherwise>
		</c:choose>
		</div>
	</c:otherwise>
</c:choose>

	</section>

    <c:if test="${not empty otherCourses}">
        <section class="progress-other" aria-labelledby="other-title">
            <h2 class="tx-section__title" id="other-title">Other planned courses</h2>
            <p class="tx-group__note">Planned courses outside your suggested sequence.</p>
            <ul class="tx-list"><c:forEach var="course" items="${otherCourses}"><cc:auditCourseRow course="${course}" meta=""/></c:forEach></ul>
        </section>
    </c:if>
    <c:if test="${not anyDone}"><p class="tx-empty">No completed courses yet. Upload your transcript or add completed courses.</p></c:if>
    <a class="course-link progress-browse" href="${context}/catalog.jsp">Browse catalog to plan a course</a>
    <form id="import-form" method="post" action="${context}/transcript" hidden>
        <input type="hidden" name="q" value="<c:out value='${query}'/>">
        <input type="hidden" name="area" value="<c:out value='${areaKey}'/>">
        <input type="hidden" name="imported" value="0">
    </form>
    <script type="application/json" id="tx-courses">${coursesJson}</script>
    <script type="application/json" id="tx-majors" data-current="<c:out value='${currentMajorId}'/>">${majorsJson}</script>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
<c:set var="toastText" value="${savedMessage}" scope="request"/>
<%@ include file="/WEB-INF/jspf/toast.jspf" %>
<script src="${context}/js/toast.js" defer></script>
</body>
</html>
