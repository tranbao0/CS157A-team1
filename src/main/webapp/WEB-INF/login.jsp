<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Sign-in form, LoginServlet handles POST and sends back error and email --%>
<%-- c:out escapes HTML, so typed email cant inject tags into page --%>
<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Sign in - CourseCraft</title>
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
	<h1 class="pagehead__title">Sign in</h1>

<c:if test="${not empty error}">
	<div class="banner banner--error" role="alert"><c:out value="${error}"/></div>
</c:if>

	<form method="post" action="<%= request.getContextPath() %>/login" class="stack">
		<div class="field">
			<label for="email">Email</label>
			<input type="email" id="email" name="email" autocomplete="username"
			       value="<c:out value='${email}'/>" required>
		</div>

		<div class="field">
			<label for="password">Password</label>
			<input type="password" id="password" name="password" autocomplete="current-password" required>
		</div>

		<button type="submit" class="btn btn--primary btn--block">Sign in</button>
	</form>
</main>

</body>
</html>
