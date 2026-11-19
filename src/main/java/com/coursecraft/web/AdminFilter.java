package com.coursecraft.web;

import java.io.IOException;
import java.sql.SQLException;

import com.coursecraft.dao.StudentDao;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebFilter({ "/admin", "/admin/*" })
public class AdminFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		HttpSession session = httpRequest.getSession(false);
		Integer studentId = null;
		if (session != null) {
			studentId = (Integer) session.getAttribute("studentId");
		}
		if (studentId == null) {
			AuthFilter.handleSignedOut(httpRequest, httpResponse);
			return;
		}

		String email = (String) session.getAttribute("email");
		if (email == null) {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		boolean admin;
		try {
			admin = new StudentDao().isAdmin(studentId, email);
		} catch (SQLException e) {
			throw new ServletException("Could not check admin access.", e);
		}
		if (!admin) {
			httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		chain.doFilter(request, response);
	}
}
