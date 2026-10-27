<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Sign-in and sign-up forms on one page, LoginServlet and RegisterServlet both forward here --%>
<%-- authMode says which form shows first, auth.js switches between them without new page load --%>
<%-- c:out escapes HTML, so typed email cant inject tags into page --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
<c:choose>
	<c:when test="${authMode == 'register'}">
	<title>Create account - CourseCraft</title>
	</c:when>
	<c:otherwise>
	<title>Sign in - CourseCraft</title>
	</c:otherwise>
</c:choose>
	<%@ include file="/WEB-INF/jspf/head.jspf" %>
	<script src="<%= request.getContextPath() %>/js/header.js" defer></script>
	<script src="<%= request.getContextPath() %>/js/auth.js" defer></script>
</head>
<body>

<a class="skip-link" href="#main">Skip to main content</a>

<%@ include file="/WEB-INF/jspf/road.jspf" %>

<% request.setAttribute("navPage", "auth"); %>
<header class="site-header">
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
</header>

<main class="narrow auth" id="main">
	<%-- hidden attribute keeps other form out of view and away from screen readers --%>
	<div class="auth__pane" data-auth-pane="login" data-auth-title="Sign in - CourseCraft"
	     <c:if test="${authMode == 'register'}">hidden</c:if>>
		<%-- tabindex -1 so auth.js can move focus here after switch --%>
		<h1 class="narrow__title" id="login-title" tabindex="-1">Sign in</h1>

	<c:if test="${authMode != 'register' and not empty error}">
		<div class="banner banner--error" role="alert" data-auth-error><c:out value="${error}"/></div>
	</c:if>

		<form method="post" action="<%= request.getContextPath() %>/login" class="stack">
			<%-- page student wanted before login, LoginServlet only sets it when path is safe --%>
			<c:if test="${not empty next}">
				<input type="hidden" name="next" value="<c:out value='${next}'/>">
			</c:if>
			<div class="field">
				<label class="field__label" for="login-email">Email</label>
				<input class="field__input" type="email" id="login-email" name="email" autocomplete="username"
				       value="<c:if test="${authMode != 'register'}"><c:out value='${email}'/></c:if>" required>
			</div>

			<div class="field">
				<label class="field__label" for="login-password">Password</label>
				<input class="field__input" type="password" id="login-password" name="password" autocomplete="current-password" required>
				<%-- empty line, same height as sign-up hint, so card does not jump on switch --%>
				<p class="field__hint" aria-hidden="true">&nbsp;</p>
			</div>

			<button type="submit" class="btn btn--primary btn--block">Sign in</button>
		</form>

		<p class="note">No account yet? <a href="<%= request.getContextPath() %>/register" data-auth-switch="register">Create account</a></p>
	</div>

	<div class="auth__pane" data-auth-pane="register" data-auth-title="Create account - CourseCraft"
	     <c:if test="${authMode != 'register'}">hidden</c:if>>
		<h1 class="narrow__title" id="register-title" tabindex="-1">Create account</h1>

	<c:if test="${authMode == 'register' and not empty error}">
		<div class="banner banner--error" role="alert" data-auth-error><c:out value="${error}"/></div>
	</c:if>

		<form method="post" action="<%= request.getContextPath() %>/register" class="stack">
			<div class="field">
				<label class="field__label" for="register-email">Email</label>
				<input class="field__input" type="email" id="register-email" name="email" autocomplete="username" maxlength="160"
				       value="<c:if test="${authMode == 'register'}"><c:out value='${email}'/></c:if>" required>
			</div>

			<div class="field">
				<label class="field__label" for="register-password">Password</label>
				<%-- server checks too, these only give quicker feedback --%>
				<%-- aria-describedby makes screen readers read hint together with field --%>
				<input class="field__input" type="password" id="register-password" name="password" autocomplete="new-password" minlength="8" required
				       aria-describedby="register-password-hint">
				<p class="field__hint" id="register-password-hint">Use 8 to 72 characters.</p>
			</div>

			<button type="submit" class="btn btn--primary btn--block">Create account</button>
		</form>

		<p class="note">Already have an account? <a href="<%= request.getContextPath() %>/login" data-auth-switch="login">Sign in</a></p>
	</div>
</main>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>

</body>
</html>
