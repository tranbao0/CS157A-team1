<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="courses" required="true" type="java.util.List" %>
<c:set var="completedCount" value="0"/>
<c:forEach var="course" items="${courses}"><c:if test="${course.passed}"><c:set var="completedCount" value="${completedCount + 1}"/></c:if></c:forEach>
<p class="completed-count">${completedCount} ${completedCount == 1 ? 'course' : 'courses'} completed</p>
<c:choose>
    <c:when test="${completedCount == 0}"><p class="tx-empty">No completed courses yet. Upload your transcript or add completed courses on My progress.</p></c:when>
    <c:otherwise>
        <ul class="completed-list" aria-label="Completed courses">
            <c:forEach var="course" items="${courses}">
                <c:if test="${course.passed}">
                    <li class="completed-course" data-completed-course="${course.courseId}">
                        <span class="completed-course-mark" aria-hidden="true">✓</span>
                        <input type="hidden" name="shown" value="${course.courseId}" form="transcript-form">
                        <input class="progress-check" type="checkbox" id="complete-${course.courseId}" name="completed" value="${course.courseId}" checked form="transcript-form" aria-label="Completed: ${fn:escapeXml(course.code)}">
                        <a class="course-link" href="${pageContext.request.contextPath}/course.jsp?id=${course.courseId}"><strong><c:out value="${course.code}"/></strong><span><c:out value="${course.title}"/></span></a>
                    </li>
                </c:if>
            </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>
