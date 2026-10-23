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

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String LOGIN_VIEW = "/WEB-INF/login.jsp";

	/*
	 * Real bcrypt hash of value no student has. Unknown email still runs checkpw on this,
	 * so both paths take same time and timing cant reveal which emails exist.
	 * Made with Passwords.hash, so it uses same cost as real hashes.
	 */
	private static final String DUMMY_HASH = Passwords.hash("no-such-student");

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
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
		response.sendRedirect(request.getContextPath() + "/catalog.jsp");
	}

	// back to form with error and typed email kept
	private void showForm(HttpServletRequest request, HttpServletResponse response,
			String email, String error) throws ServletException, IOException {
		request.setAttribute("email", email);
		request.setAttribute("error", error);
		request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
	}
}
