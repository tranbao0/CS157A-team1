<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Sign-up form, RegisterServlet handles POST and sends back error and email --%>
<%-- c:out escapes HTML, so typed email cant inject tags into page --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Create account - CourseCraft</title>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap">
	<link rel="stylesheet" href="<%= request.getContextPath() %>/css/app.css">
</head>
<body>

<header class="topbar">
	<a class="topbar__brand" href="<%= request.getContextPath() %>/">CourseCraft</a>
</header>

<main class="narrow">
	<h1 class="pagehead__title">Create account</h1>

<c:if test="${not empty error}">
	<div class="banner banner--error" role="alert"><c:out value="${error}"/></div>
</c:if>

	<form method="post" action="<%= request.getContextPath() %>/register" class="stack">
		<div class="field">
			<label for="email">Email</label>
			<input type="email" id="email" name="email" autocomplete="username" maxlength="160"
			       value="<c:out value='${email}'/>" required>
		</div>

		<div class="field">
			<label for="password">Password</label>
			<%-- server checks too, these only give quicker feedback --%>
			<input type="password" id="password" name="password" autocomplete="new-password" minlength="8" required>
		</div>

		<button type="submit" class="btn btn--primary btn--block">Create account</button>
	</form>

	<p class="note">Already have an account? <a href="<%= request.getContextPath() %>/login">Sign in</a></p>
</main>

</body>
</html>
