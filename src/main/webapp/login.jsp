<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Sign-in form. Handles its own POST. --%>
<%-- TODO: check the password --%>
<%
	String error = null;
	String email = "";

	if ("POST".equalsIgnoreCase(request.getMethod())) {
		email = request.getParameter("email");
		email = email == null ? "" : email.trim();

		// Password not checked.
		if (email.isEmpty()) {
			error = "Enter your email address.";
		} else {
			// New session, so a pre-login ID cannot be reused.
			request.getSession().invalidate();
			request.getSession(true).setAttribute("email", email);

			response.sendRedirect(request.getContextPath() + "/");
			return;
		}
	}
%>
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

<% if (error != null) { %>
	<div class="banner banner--error" role="alert"><%= error %></div>
<% } %>

	<form method="post" action="<%= request.getContextPath() %>/login.jsp" class="stack">
		<div class="field">
			<label for="email">Email</label>
			<input type="email" id="email" name="email" autocomplete="username"
			       value="<%= email %>" required>
		</div>

		<div class="field">
			<label for="password">Password</label>
			<input type="password" id="password" name="password" autocomplete="current-password">
		</div>

		<button type="submit" class="btn btn--primary btn--block">Sign in</button>
	</form>

	<p class="note">Passwords are not checked yet. Any email signs you in.</p>
</main>

</body>
</html>
