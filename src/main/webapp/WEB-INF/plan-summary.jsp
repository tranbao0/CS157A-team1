<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="cc" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Catalog plan summary, also refreshed by GET /plan/summary after an add. --%>
<c:choose>
    <c:when test="${empty planTerms}">
        <cc:railEmpty title="Start your plan"
            text="Use + Plan in the catalog to add a course. Your saved courses appear here by term."
            href="#q" action="Find a course"/>
    </c:when>
    <c:otherwise>
        <ol class="plan-terms" tabindex="0" aria-label="Planned terms">
			<c:forEach var="term" items="${planTerms}">
				<li class="plan-term<c:if test="${term.gradTerm}"> plan-term--grad</c:if>">
					<div class="plan-term__head">
						<h3 class="plan-term__name"><c:out value="${term.label}"/></h3>
						<span class="plan-term__units"><c:out value="${term.unitsText}"/> units</span>
					</div>
					<%-- open by default, plan-summary.js closes ones student minimized before --%>
					<c:forEach var="warning" items="${term.warnings}">
						<details class="plan-warn plan-warn--<c:out value='${warning.kind}'/>"
						         data-warn-key="<c:out value='${warning.key}'/>" open>
							<summary class="plan-warn__summary">
								<span class="plan-warn__title"><c:out value="${warning.title}"/></span>
								<span class="plan-warn__toggle"><span class="plan-warn__hide">Minimize</span><span class="plan-warn__show">Show</span></span>
							</summary>
							<p class="plan-warn__text"><c:out value="${warning.text}"/></p>
						</details>
					</c:forEach>
					<ul class="plan-term__courses">
						<c:forEach var="course" items="${term.courses}">
							<li class="plan-course">
								<span class="plan-course__code"><c:out value="${course.code}"/></span>
								<c:if test="${not empty course.riskLevel}">
									<span class="risk risk--<c:out value='${fn:toLowerCase(course.riskLevel)}'/>"><c:out value="${course.riskLevel}"/> risk</span>
								</c:if>
								<c:if test="${not empty course.petitionTermLabel}">
									<span class="plan-course__dep">Needs <c:out value="${course.petitionTermLabel}"/> petition, via <c:out value="${course.throughCourses}"/></span>
								</c:if>
							</li>
						</c:forEach>
					</ul>
				</li>
			</c:forEach>
        </ol>
        <a class="btn btn--block" href="${pageContext.request.contextPath}/plan">Open my plan</a>
    </c:otherwise>
</c:choose>
