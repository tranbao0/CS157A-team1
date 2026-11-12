<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- one roadmap course row in remaining requirements, code over title like transcript rows --%>
<%@ attribute name="course" required="true" type="com.coursecraft.model.RoadmapCourse" %>
<%@ attribute name="meta" required="true" %>
<c:set var="context" value="${pageContext.request.contextPath}"/>
				<li class="tx-row">
					<span class="tx-label tx-label--static">
						<span class="tx-code"><c:out value="${course.code}"/></span>
						<span class="tx-title"><c:out value="${course.title}"/></span>
						<span class="tx-meta"><c:out value="${meta}"/></span>
					</span>
					<span class="tx-units"><c:out value="${course.units}"/> units</span>
					<a class="course-link tx-view" href="${context}/course.jsp?id=${course.courseId}">Details</a>
				</li>
