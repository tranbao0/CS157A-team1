<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="course" required="true" type="java.lang.Object" %>
<%@ attribute name="meta" required="true" %>
<%@ attribute name="search" required="false" type="java.lang.Boolean" %>
<c:set var="context" value="${pageContext.request.contextPath}"/>
<c:set var="plans" value="${requestScope.coursePlans[course.courseId]}"/>
<li class="tx-row rm-row ${course.passed ? 'is-done' : (not empty plans ? 'is-planned' : '')} ${search ? 'progress-search-row' : ''}" id="progress-course-${course.courseId}" data-progress-course="${course.courseId}">
    <span class="rm-mark" aria-hidden="true"><c:if test="${course.passed}">✓</c:if><c:if test="${not course.passed and not empty plans}"><svg viewBox="0 0 16 16" aria-hidden="true"><rect x="3" y="4" width="10" height="9" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M3 7h10M6 2.5v3M10 2.5v3" stroke="currentColor" stroke-width="1.6"/></svg></c:if></span>
    <c:choose>
        <c:when test="${course.passed}"><a class="progress-completion-edit course-link" href="#complete-${course.courseId}" data-edit-completed="${course.courseId}">Edit<span class="visually-hidden"> completion for <c:out value="${course.code}"/></span></a></c:when>
        <c:otherwise>
            <input type="hidden" name="shown" value="${course.courseId}" form="transcript-form">
            <input class="progress-check" type="checkbox" id="complete-${course.courseId}" name="completed" value="${course.courseId}" form="transcript-form" aria-label="Completed: ${fn:escapeXml(course.code)}">
        </c:otherwise>
    </c:choose>
    <div class="tx-label tx-label--static">
        <a class="course-link progress-course-link" href="${context}/course.jsp?id=${course.courseId}"><span class="tx-code"><c:out value="${course.code}"/></span><span class="tx-title"><c:out value="${course.title}"/></span></a>
        <span class="tx-meta"><c:out value="${course.units}"/> units<c:if test="${not empty meta}"> · <c:out value="${meta}"/><c:if test="${meta == 'Elective'}"> option</c:if></c:if><c:if test="${course.passed}"> · Completed</c:if><c:if test="${not course.passed and empty plans}"><span class="visually-hidden"> · Not planned</span></c:if></span>
        <c:forEach var="plan" items="${plans}">
            <div class="progress-plan">
                <span class="rm-plan-meta">Planned for <c:out value="${plan.termLabel}"/></span>
                <c:if test="${not empty plan.riskLevel}"><span class="tx-meta"><c:out value="${plan.riskLevel}"/> risk · sample data</span></c:if>
                <c:if test="${not empty plan.petitionTermLabel}"><span class="plan-course__dep">Needs <c:out value="${plan.petitionTermLabel}"/> petition, via <c:out value="${plan.throughCourses}"/></span></c:if>
                <form method="post" action="${context}/plan/remove">
                    <input type="hidden" name="planEntryId" value="${plan.planEntryId}">
                    <button class="btn btn--sm btn--ghost" type="submit">Remove<span class="visually-hidden"> <c:out value="${course.code}"/> from plan for <c:out value="${plan.termLabel}"/></span></button>
                </form>
            </div>
        </c:forEach>
    </div>
</li>
