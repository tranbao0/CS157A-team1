<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="text" required="true" %>
<%@ attribute name="href" required="true" %>
<%@ attribute name="action" required="true" %>
<div class="rail-empty">
    <h3 class="card__subtitle"><c:out value="${title}"/></h3>
    <p class="card__body"><c:out value="${text}"/></p>
    <a class="btn btn--block" href="<c:out value='${href}'/>"><c:out value="${action}"/></a>
</div>
