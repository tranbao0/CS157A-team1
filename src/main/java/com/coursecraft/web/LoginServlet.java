package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

import com.coursecraft.dao.StudentDao;
import com.coursecraft.model.Student;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// GET shows form, POST checks it
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	// inside WEB-INF so browser cant open it directly, only this servlet can forward to it
	private static final String LOGIN_VIEW = "/WEB-INF/login.jsp";

	/*
	 * Real bcrypt hash of value no student has. Unknown email still runs checkpw on this,
	 * so both paths take same time and timing cant reveal which emails exist.
	 * Cost 10 must match cost of stored hashes (seed.sql uses 10).
	 */
	private static final String DUMMY_HASH = BCrypt.hashpw("no-such-student", BCrypt.gensalt(10));

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
		// checkpw hashes typed password with salt stored in hash and compares
		boolean passwordMatches = BCrypt.checkpw(password, hashToCheck);

		// same message for both cases, otherwise anyone could test which emails have accounts
		if (student == null || !passwordMatches) {
			showForm(request, response, email, "Incorrect email or password.");
			return;
		}

		startSession(request, student);
		response.sendRedirect(request.getContextPath() + "/catalog.jsp");
	}

	// any login type (password now, Google later) ends here
	private void startSession(HttpServletRequest request, Student student) {
		HttpSession oldSession = request.getSession(false);
		if (oldSession != null) {
			// fresh session so pre-login ID cant be reused
			oldSession.invalidate();
		}
		HttpSession session = request.getSession(true);
		session.setAttribute("studentId", student.getStudentId());
		session.setAttribute("email", student.getEmail());
	}

	// back to form with error and typed email kept
	private void showForm(HttpServletRequest request, HttpServletResponse response,
			String email, String error) throws ServletException, IOException {
		request.setAttribute("email", email);
		request.setAttribute("error", error);
		request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
	}
}
