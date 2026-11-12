<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="buttonClass" required="true" type="java.lang.String" %>
<%@ attribute name="courseId" required="true" type="java.lang.String" %>
<%@ attribute name="sessionId" required="true" type="java.lang.String" %>
<%@ attribute name="actionId" required="true" type="java.lang.String" %>
<%@ attribute name="signInHref" required="true" type="java.lang.String" %>
<%@ attribute name="returnTo" required="true" type="java.lang.String" %>
<%@ attribute name="signedIn" required="true" type="java.lang.Boolean" %>
<%@ attribute name="availability" required="false" type="com.coursecraft.service.PlanAvailability" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    int course = Integer.parseInt(courseId);
    int targetSession = Integer.parseInt(sessionId);
    String reason = "Could not check prerequisites. Reload the page to try again.";
    if (availability != null) {
        reason = availability.reason(course, targetSession);
    }
    jspContext.setAttribute("reason", reason);
    jspContext.setAttribute("done", availability != null && availability.isCompleted(course));
    jspContext.setAttribute("planned", availability != null && availability.isInPlan(course));
%>
<div class="plan-action" id="<c:out value='${actionId}'/>" data-plan-action>
<c:choose>
    <c:when test="${!signedIn}">
        <a class="<c:out value='${buttonClass}'/>" href="<c:out value='${signInHref}'/>">+ Plan<span class="visually-hidden">, sign in to add this course</span></a>
    </c:when>
    <c:when test="${done}"><button class="<c:out value='${buttonClass}'/> is-added" type="button" disabled>Completed</button></c:when>
    <c:when test="${planned}"><button class="<c:out value='${buttonClass}'/> is-added" type="button" disabled>In plan</button></c:when>
    <c:otherwise>
        <form method="post" action="${pageContext.request.contextPath}/plan/add" class="plan-action__form">
            <input type="hidden" name="courseId" value="<c:out value='${courseId}'/>">
            <input type="hidden" name="sessionId" value="<c:out value='${sessionId}'/>">
            <input type="hidden" name="returnTo" value="<c:out value='${returnTo}'/>">
            <button class="<c:out value='${buttonClass}'/> js-plan-add" type="submit"
                data-course-id="<c:out value='${courseId}'/>" data-session-id="<c:out value='${sessionId}'/>"
                <c:if test="${!empty reason}">disabled aria-disabled="true" aria-describedby="<c:out value='${actionId}'/>-reason"</c:if>>+ Plan</button>
        </form>
        <c:if test="${!empty reason}"><p tabindex="0" class="plan-action__reason" id="<c:out value='${actionId}'/>-reason"><c:out value="${reason}"/></p></c:if>
    </c:otherwise>
</c:choose>
</div>
