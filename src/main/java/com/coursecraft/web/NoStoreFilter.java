package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * Every page shows session data: top bar has student's email, catalog has plan card.
 * no-store tells browser to keep no copy, so Back button after sign-out asks Tomcat again
 * and gets signed-out page. Same for JSON that page scripts fetch.
 * Anything outside static folders counts as page, so every new JSP or servlet gets no-store.
 * ERROR is in dispatcher list for error page: Tomcat sends it for missing file in static folder too,
 * and error page has top bar like every other page.
 * Static files skip this, StaticCacheFilter and Tomcat handle those.
 */
@WebFilter(urlPatterns = "/*", dispatcherTypes = { DispatcherType.REQUEST, DispatcherType.ERROR })
public class NoStoreFilter implements Filter {

	// files in these folders are same for everyone, no session data in them
	private static final String[] STATIC_FOLDERS = { "/css/", "/js/", "/fonts/", "/images/" };

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		// servlet path is address inside app, without app name and without ;jsessionid part
		if (!isStaticFile(httpRequest.getServletPath())) {
			// set before chain, page starts writing body inside chain
			httpResponse.setHeader("Cache-Control", "no-store");
		}
		chain.doFilter(request, response);
	}

	// true when path starts with one of static folders, like /css/app.css
	private boolean isStaticFile(String path) {
		for (String folder : STATIC_FOLDERS) {
			if (path.startsWith(folder)) {
				return true;
			}
		}
		return false;
	}
}
