package com.smartstudy.web;

import author.*;
import com.smartstudy.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public final class AIController {
    private AIController() {}

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("ai-settings")) return false;
        User u = (User) req.getSession().getAttribute("user");
        if (u.getRole() == Role.STUDENT) {
            resp.sendError(403);
            return true;
        }
        var ai = AIService.getInstance();
        req.setAttribute("aiProvider", ai.providerLabel());
        req.setAttribute(
                "aiStatus",
                new com.smartstudy.model.view.AIStatus(
                        ai.configured(),
                        ai.enabled(),
                        ai.limit(),
                        ai.successes(),
                        ai.failures(),
                        ai.lastState()));
        req.getRequestDispatcher("/WEB-INF/views/shared/ai-settings.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("ai-settings")) return false;
        User u = (User) req.getSession().getAttribute("user");
        if (u.getRole() != Role.ADMIN || !ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return true;
        }
        try {
            AIService.getInstance()
                    .configure(
                            u,
                            "on".equals(req.getParameter("enabled")),
                            Integer.parseInt(req.getParameter("limit")));
            req.getSession().setAttribute("flash", "Đã lưu chính sách sử dụng AI.");
            resp.sendRedirect(req.getContextPath() + "/admin/ai-settings");
        } catch (IllegalArgumentException e) {
            req.setAttribute(
                    "aiError",
                    e instanceof NumberFormatException ? "Giới hạn phải là số." : e.getMessage());
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
