<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="course" required="true" type="com.coursecraft.model.RoadmapCourse" %>
<%@ attribute name="meta" required="true" %>
<c:set var="context" value="${pageContext.request.contextPath}"/>
<c:set var="rowState" value=""/>
<c:choose>
 <c:when test="${course.passed}"><c:set var="rowState" value="is-done"/></c:when>
 <c:when test="${not empty course.plannedTermLabel}"><c:set var="rowState" value="is-planned"/></c:when>
</c:choose>
<li class="tx-row rm-row ${rowState}">
 <span class="rm-mark" aria-hidden="true"><c:choose><c:when test="${course.passed}">✓</c:when><c:when test="${not empty course.plannedTermLabel}"><svg viewBox="0 0 16 16" aria-hidden="true"><rect x="3" y="4" width="10" height="9" rx="1.5" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M3 7h10M6 2.5v3M10 2.5v3" stroke="currentColor" stroke-width="1.6"/></svg></c:when></c:choose></span>
 <span class="tx-label tx-label--static">
  <span class="tx-code"><c:out value="${course.code}"/></span>
  <span class="tx-title"><c:out value="${course.title}"/></span>
  <span class="tx-meta"><c:out value="${meta}"/><c:if test="${meta == 'Elective'}"> option</c:if><c:if test="${not course.passed and not empty course.plannedTermLabel}"> · <span class="rm-plan-meta">Planned for <c:out value="${course.plannedTermLabel}"/></span></c:if></span>
 </span>
 <c:choose>
  <c:when test="${course.passed}"><span class="rm-done">Completed</span></c:when>
  <c:when test="${not empty course.plannedTermLabel}"><span class="rm-done rm-done--plan">Planned</span></c:when>
  <c:otherwise><span class="visually-hidden">Not planned</span></c:otherwise>
 </c:choose>
 <span class="tx-units"><c:out value="${course.units}"/> units</span>
 <a class="course-link tx-view" href="${context}/course.jsp?id=${course.courseId}">Details</a>
</li>
