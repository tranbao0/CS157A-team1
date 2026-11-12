package com.coursecraft.web;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Plan removal uses the same progress page and one-time message as normal page loads.
public final class PlanPage {
    public static final String FLASH = "planMessage";

    private PlanPage() {
    }

    public static void show(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        request.setAttribute("planError", error);
        TranscriptServlet.show(request, response);
    }
}
