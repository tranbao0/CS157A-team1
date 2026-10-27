<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- shows plan card body, catalog.jsp includes it on load and PlanSummaryServlet sends it after each add --%>
<%-- My plan page includes it too with planView "page", so rail and page always show same plan --%>
<%-- c:out escapes HTML, so course names from database cant inject tags --%>
<c:set var="pageMode" value="${planView == 'page'}"/>
<c:choose>
	<c:when test="${empty planTerms}">
		<%-- page has h1 above, rail card has "Your plan" h2, so empty-state heading sits one level below either --%>
		<c:choose>
			<c:when test="${pageMode}">
				<h2 class="card__title">Nothing saved yet</h2>
			</c:when>
			<c:otherwise>
				<h3 class="card__subtitle">Nothing saved yet</h3>
			</c:otherwise>
		</c:choose>
		<p class="card__body">Use + Plan on a course row to add it to a term.</p>
		<c:if test="${pageMode}">
			<a class="btn btn--primary" href="${pageContext.request.contextPath}/catalog.jsp">Browse catalog</a>
		</c:if>
	</c:when>
	<c:otherwise>
		<%-- rail list scrolls inside small box, so keyboard needs tabindex to reach it, page list does not scroll --%>
		<ol class="plan-terms<c:if test='${pageMode}'> plan-terms--page</c:if>"<c:if test='${not pageMode}'> tabindex="0"</c:if> aria-label="Planned terms">
			<c:forEach var="term" items="${planTerms}">
				<li class="plan-term<c:if test="${term.gradTerm}"> plan-term--grad</c:if>">
					<div class="plan-term__head">
						<%-- page has h1 above, rail card has h2 title, so term heading sits one level below either --%>
						<c:choose>
							<c:when test="${pageMode}">
								<%-- star marker is drawing only, hidden text tells screen readers same thing --%>
								<h2 class="plan-term__name"><c:out value="${term.label}"/><c:if test="${term.gradTerm}"><span class="visually-hidden">, expected graduation</span></c:if></h2>
							</c:when>
							<c:otherwise>
								<h3 class="plan-term__name"><c:out value="${term.label}"/></h3>
							</c:otherwise>
						</c:choose>
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
								<%-- full page has room for course name, narrow rail card shows code only --%>
								<c:if test="${pageMode}">
									<span class="plan-course__title"><c:out value="${course.title}"/></span>
								</c:if>
								<c:if test="${not empty course.riskLevel}">
									<span class="risk risk--<c:out value='${fn:toLowerCase(course.riskLevel)}'/>"><c:out value="${course.riskLevel}"/> risk</span>
								</c:if>
								<%-- only page mode gets Remove form, plain POST works without JavaScript and PlanRemoveServlet checks owner and dependents --%>
								<c:if test="${pageMode}">
									<form class="plan-course__remove" method="post" action="${pageContext.request.contextPath}/plan/remove">
										<input type="hidden" name="planEntryId" value="${course.planEntryId}">
										<button class="btn btn--sm btn--ghost" type="submit">Remove<span class="visually-hidden"> <c:out value="${course.code}"/> from plan</span></button>
									</form>
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
		<%-- page is already My plan, so link only shows in rail card --%>
		<c:if test="${not pageMode}">
			<a class="btn btn--block" href="${pageContext.request.contextPath}/plan">Open my plan</a>
		</c:if>
	</c:otherwise>
</c:choose>
