package com.coursecraft.web;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

/*
 * Tomcat sends static files with no Cache-Control header, so browsers guess how long
 * a file stays fresh, often hours. Then after CSS or JS change, page shows old styles.
 * no-cache means browser still keeps file, but asks Tomcat first every time.
 * Unchanged file costs one small 304 Not Modified reply, changed file comes back new.
 */
@WebFilter({ "*.css", "*.js", "*.html" })
public class StaticCacheFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		// set before chain, Tomcat starts writing file body inside chain
		httpResponse.setHeader("Cache-Control", "no-cache");
		chain.doFilter(request, response);
	}
}
