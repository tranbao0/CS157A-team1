<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- one GE area row in remaining requirements, Find courses jumps to search when catalog has courses for area --%>
<%@ attribute name="area" required="true" type="com.coursecraft.model.RoadmapGeStatus" %>
<c:set var="context" value="${pageContext.request.contextPath}"/>
				<li class="tx-row">
					<span class="tx-label tx-label--static">
						<span class="tx-code">Area <c:out value="${area.code}"/></span>
						<span class="tx-title"><c:out value="${area.name}"/></span>
					</span>
					<c:choose>
						<c:when test="${area.hasCourses}">
							<c:url var="findUrl" value="/transcript"><c:param name="area" value="ge-${area.code}"/></c:url>
							<a class="course-link tx-view" href="${findUrl}#tx-search-title">Find courses</a>
						</c:when>
						<c:otherwise>
							<span class="tx-note">Met with science lab</span>
						</c:otherwise>
					</c:choose>
				</li>
