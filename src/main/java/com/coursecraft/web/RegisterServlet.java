package com.coursecraft.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.Student;
import com.coursecraft.util.Passwords;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// GET shows sign-up form, POST creates account
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// names session key catalog.jsp reads to open get-started dialog right after sign-up
	public static final String GET_STARTED_FLAG = "showGetStarted";

	// inside WEB-INF so browser cant open it directly, only servlets can forward to it
	// same page as sign-in, authMode tells it to show sign-up form first
	private static final String AUTH_VIEW = "/WEB-INF/auth.jsp";

	// same as students.email VARCHAR(160) in schema.sql, longer would fail on insert
	private static final int MAX_EMAIL_LENGTH = 160;

	// something@something.something, no spaces, only one @
	// loose on purpose, only real proof would be sending email to it
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// already signed in, so new account form makes no sense, send student to catalog
		if (LoginSession.isSignedIn(request)) {
			response.sendRedirect(request.getContextPath() + "/catalog.jsp");
			return;
		}

		request.setAttribute("authMode", "register");
		request.getRequestDispatcher(AUTH_VIEW).forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
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

		// browser checks type="email" too, but anyone can skip browser, so check again here
		if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
			showForm(request, response, email, "Enter a valid email address.");
			return;
		}

		// max counts bytes not characters, accented letters and emoji take more than 1 byte
		int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
		if (password.length() < Passwords.MIN_LENGTH || passwordBytes > Passwords.MAX_BYTES) {
			showForm(request, response, email,
					"Password must be " + Passwords.MIN_LENGTH + " to " + Passwords.MAX_BYTES + " characters.");
			return;
		}

		String passwordHash = Passwords.hash(password);

		Student student;
		try {
			student = new StudentDao().create(email, passwordHash);
		} catch (SQLIntegrityConstraintViolationException e) {
			/*
			 * email column is UNIQUE, so MySQL refuses second account with same email.
			 * Letting insert fail is safer than checking first with SELECT,
			 * two sign-ups at same moment could both pass SELECT.
			 */
			showForm(request, response, email, "An account with that email already exists.");
			return;
		} catch (SQLException e) {
			throw new ServletException("Could not create student.", e);
		}

		// new account is signed in right away, no second login needed
		LoginSession.start(request, student);
		// catalog sees this flag once and opens get-started dialog with profile form
		request.getSession().setAttribute(GET_STARTED_FLAG, Boolean.TRUE);
		response.sendRedirect(request.getContextPath() + "/catalog.jsp");
	}

	// back to form with error and typed email kept
	private void showForm(HttpServletRequest request, HttpServletResponse response,
			String email, String error) throws ServletException, IOException {
		request.setAttribute("email", email);
		request.setAttribute("error", error);
		request.setAttribute("authMode", "register");
		request.getRequestDispatcher(AUTH_VIEW).forward(request, response);
	}
}
