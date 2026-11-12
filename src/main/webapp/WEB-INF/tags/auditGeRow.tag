<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- GE status from roadmap audit, search link for unmet area --%>
<%@ attribute name="area" required="true" type="com.coursecraft.model.RoadmapGeStatus" %>
<c:set var="context" value="${pageContext.request.contextPath}"/>
				<li class="tx-row rm-row ${area.fulfilled ? 'is-done' : ''}">
					<span class="rm-mark" aria-hidden="true"><c:if test="${area.fulfilled}">✓</c:if></span>
					<span class="tx-label tx-label--static">
						<span class="tx-code">Area <c:out value="${area.code}"/></span>
						<span class="tx-title"><c:out value="${area.name}"/></span>
						<span class="tx-meta">
							<c:choose>
								<c:when test="${area.fulfilled}">Met by <c:out value="${area.fulfilledByCode}"/></c:when>
								<c:otherwise>Not met yet</c:otherwise>
							</c:choose>
						</span>
					</span>
					<c:choose>
						<c:when test="${area.fulfilled}">
						</c:when>
						<c:when test="${area.hasCourses}">
							<c:url var="findUrl" value="/transcript"><c:param name="area" value="ge-${area.code}"/></c:url>
							<a class="course-link tx-view" href="${findUrl}#tx-search-title">Find courses</a>
						</c:when>
						<c:otherwise>
							<span class="tx-note">Requires a science lab</span>
						</c:otherwise>
					</c:choose>
				</li>
