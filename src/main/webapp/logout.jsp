<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%
	// Drop session. TODO: should be POST.
	request.getSession().invalidate();
	response.sendRedirect(request.getContextPath() + "/");
%>
