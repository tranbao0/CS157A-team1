package com.coursecraft.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/*
 * Runs before every request to pages that need signed-in student.
 * Add new protected paths to list below, servlet itself needs no login code then.
 * /plan/* also covers later routes like POST /plan/add.
 */
@WebFilter({ "/plan", "/plan/*" })
public class AuthFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// filter gets general request types, cast to HTTP ones to use sessions and redirects
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		// false so we dont make new session just to check it
		HttpSession session = httpRequest.getSession(false);
		boolean signedIn = session != null && session.getAttribute("studentId") != null;

		if (!signedIn) {
			/*
			 * Browser loading page sends Accept: text/html, fetch() from JavaScript does not.
			 * Page loads go to sign-in form. Script calls get 401 so script can react,
			 * redirect would only hand script HTML of login page.
			 */
			String accept = httpRequest.getHeader("Accept");
			boolean pageLoad = accept != null && accept.contains("text/html");
			if (!pageLoad) {
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			String loginUrl = httpRequest.getContextPath() + "/login";
			// GET only, browser can open same address again after login, POST cant be repeated that way
			if ("GET".equals(httpRequest.getMethod())) {
				String next = pathInsideApp(httpRequest);
				loginUrl = loginUrl + "?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8);
			}
			httpResponse.sendRedirect(loginUrl);
			return;
		}

		// private page, browser must not keep copy that Back button could show after sign-out
		httpResponse.setHeader("Cache-Control", "no-store");

		// signed in, let request go on to servlet
		chain.doFilter(request, response);
	}

	// address without app name, /coursecraft/plan?term=3 becomes /plan?term=3
	private String pathInsideApp(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		String query = request.getQueryString();
		if (query != null) {
			path = path + "?" + query;
		}
		return path;
	}
}
