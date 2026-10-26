package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.Student;
import com.coursecraft.util.Passwords;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// GET shows form, POST checks it
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only servlets can forward to it
	// same page as sign-up, authMode tells it to show sign-in form first
	private static final String AUTH_VIEW = "/WEB-INF/auth.jsp";

	/*
	 * Real bcrypt hash of value no student has. Unknown email still runs checkpw on this,
	 * so both paths take same time and timing cant reveal which emails exist.
	 * Made with Passwords.hash, so it uses same cost as real hashes.
	 */
	private static final String DUMMY_HASH = Passwords.hash("no-such-student");

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String next = safeNext(request.getParameter("next"));

		// already signed in, so skip form and go where sign-in would have gone
		if (LoginSession.isSignedIn(request)) {
			if (next != null) {
				response.sendRedirect(request.getContextPath() + next);
			} else {
				response.sendRedirect(request.getContextPath() + "/catalog.jsp");
			}
			return;
		}

		// AuthFilter sends ?next=/plan, form keeps it in hidden field for POST
		request.setAttribute("next", next);
		request.setAttribute("authMode", "login");
		request.getRequestDispatcher(AUTH_VIEW).forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// set early so form keeps it when it comes back with error
		String next = safeNext(request.getParameter("next"));
		request.setAttribute("next", next);

		// field missing gives null, so treat it as empty
		String email = request.getParameter("email");
		if (email == null) {
			email = "";
		} else {
			email = email.trim();
		}

		// no trim here, spaces can be part of password
		String password = request.getParameter("password");
		if (password == null) {
			password = "";
		}

		if (email.isEmpty() || password.isEmpty()) {
			showForm(request, response, email, "Enter your email and password.");
			return;
		}

		Student student;
		try {
			student = new StudentDao().findByEmail(email);
		} catch (SQLException e) {
			throw new ServletException("Could not look up student.", e);
		}

		// always run one bcrypt check, even when email not found (see DUMMY_HASH)
		String hashToCheck;
		if (student == null) {
			hashToCheck = DUMMY_HASH;
		} else {
			hashToCheck = student.getPasswordHash();
		}
		boolean passwordMatches = Passwords.matches(password, hashToCheck);

		// same message for both cases, otherwise anyone could test which emails have accounts
		if (student == null || !passwordMatches) {
			showForm(request, response, email, "Incorrect email or password.");
			return;
		}

		LoginSession.start(request, student);

		// back to page student wanted before login, catalog when there was none
		if (next != null) {
			response.sendRedirect(request.getContextPath() + next);
		} else {
			response.sendRedirect(request.getContextPath() + "/catalog.jsp");
		}
	}

	/*
	 * Only accepts path inside this app like /plan, anything else gives null.
	 * Without this, link like /login?next=https://evil.example could send student
	 * to other site right after login (open redirect).
	 */
	private String safeNext(String next) {
		if (next == null || !next.startsWith("/")) {
			return null;
		}
		// browsers read //evil.example and /\evil.example as other website
		if (next.startsWith("//") || next.startsWith("/\\")) {
			return null;
		}
		// line break could sneak extra header into redirect reply
		if (next.contains("\r") || next.contains("\n")) {
			return null;
		}
		return next;
	}

	// back to form with error and typed email kept
	private void showForm(HttpServletRequest request, HttpServletResponse response,
			String email, String error) throws ServletException, IOException {
		request.setAttribute("email", email);
		request.setAttribute("error", error);
		request.setAttribute("authMode", "login");
		request.getRequestDispatcher(AUTH_VIEW).forward(request, response);
	}
}
