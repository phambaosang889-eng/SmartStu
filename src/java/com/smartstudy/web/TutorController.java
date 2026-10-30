package com.smartstudy.web;

import author.User;
import com.smartstudy.service.*;
import com.smartstudy.persistence.Database;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

public final class TutorController {
    private static final TutorService SERVICE = new TutorService();

    private TutorController() {}

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("ai-tutor")) return false;
        User u = (User) req.getSession().getAttribute("user");
        req.setAttribute("tutorConfigured", AIService.getInstance().available());
        req.setAttribute("tutorStorage", !Database.memory());
        if (!Database.memory()) {
            int listPage = 1;
            try {
                if (req.getParameter("listPage") != null)
                    listPage = Integer.parseInt(req.getParameter("listPage"));
                if (listPage < 1 || listPage > 1000) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                resp.sendError(400);
                return true;
            }
            var conversations = SERVICE.conversations(u, listPage);
            req.setAttribute("conversations", conversations.stream().limit(50).toList());
            req.setAttribute("tutorPage", listPage);
            req.setAttribute("tutorNextPage", conversations.size() > 50);
            String id = req.getParameter("id");
            if (id != null && !id.isBlank())
                try {
                    req.setAttribute("messages", SERVICE.messages(u, id));
                    req.setAttribute("conversationId", id);
                } catch (IllegalArgumentException e) {
                    resp.sendError(404);
                    return true;
                }
        }
        req.setAttribute("requestId", UUID.randomUUID().toString());
        req.getRequestDispatcher("/WEB-INF/views/shared/ai-tutor.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("ai-tutor")) return false;
        if (!ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return true;
        }
        if (Database.memory()) {
            resp.sendError(503);
            return true;
        }
        User u = (User) req.getSession().getAttribute("user");
        String id = req.getParameter("id");
        try {
            String action = req.getParameter("action");
            if ("new".equals(action)) id = SERVICE.create(u);
            else if ("hide".equals(action)) {
                SERVICE.hide(u, id);
                id = null;
            } else if ("send".equals(action))
                SERVICE.send(u, id, req.getParameter("requestId"), req.getParameter("message"));
            else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            resp.sendRedirect(
                    req.getContextPath()
                            + "/"
                            + ViewSupport.role(u)
                            + "/ai-tutor"
                            + (id == null ? "" : "?id=" + id));
        } catch (IllegalArgumentException e) {
            req.setAttribute("tutorError", e.getMessage());
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
