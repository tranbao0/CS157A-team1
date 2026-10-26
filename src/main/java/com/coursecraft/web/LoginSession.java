package com.coursecraft.web;

import com.coursecraft.model.Student;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// every way of signing in (password, sign-up, Google later) ends here
public final class LoginSession {

	private LoginSession() {
	}

	// tells if request already has signed-in student, false so check never makes new session
	public static boolean isSignedIn(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		return session != null && session.getAttribute("studentId") != null;
	}

	public static void start(HttpServletRequest request, Student student) {
		HttpSession oldSession = request.getSession(false);
		if (oldSession != null) {
			// fresh session so pre-login ID cant be reused
			oldSession.invalidate();
		}
		HttpSession session = request.getSession(true);
		session.setAttribute("studentId", student.getStudentId());
		session.setAttribute("email", student.getEmail());
	}
}
