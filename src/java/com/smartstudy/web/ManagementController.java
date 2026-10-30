package com.smartstudy.web;

import author.*;
import com.smartstudy.model.CatalogEntry;
import com.smartstudy.service.CatalogService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class ManagementController {
    private ManagementController() {}

    private static String kind(String p) {
        return p.equals("users") ? "students" : p;
    }

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User u = (User) req.getSession().getAttribute("user");
        String p = ViewSupport.current(req);
        if (p.equals("content") && u.getRole() == Role.ADMIN) {
            resp.sendRedirect(req.getContextPath() + "/admin/courses");
            return true;
        }
        if (p.equals("settings") && u.getRole() == Role.ADMIN) {
            req.getRequestDispatcher("/WEB-INF/views/shared/settings.jsp").forward(req, resp);
            return true;
        }
        if (!CatalogService.LABELS.containsKey(kind(p))) return false;
        if (u.getRole() == Role.ADMIN && !java.util.Set.of("notifications", "topics").contains(p)) {
            if ("edit".equals(req.getParameter("mode"))) {
                resp.sendError(403);
                return true;
            }
        } else
            try {
                CatalogService.authorize(kind(p), u);
            } catch (IllegalArgumentException e) {
                resp.sendError(403);
                return true;
            }
        CatalogService store = CatalogService.getInstance();
        req.setAttribute("catalogKind", kind(p));
        req.setAttribute("catalogLabel", CatalogService.LABELS.get(kind(p)));
        req.setAttribute("entries", store.list(kind(p), u));
        String id = req.getParameter("id");
        if (id != null && !id.isBlank()) {
            CatalogEntry e = store.get(id);
            if (e == null
                    || !e.kind().equals(kind(p))
                    || u.getRole() != Role.ADMIN && !e.owner().equals(u.getEmail())) {
                resp.sendError(404);
                return true;
            }
            req.setAttribute("entry", e);
            if (e.kind().equals("lesson"))
                req.setAttribute(
                        "lessonVideos",
                        new com.smartstudy.service.LessonVideoService().list(u, e.id()));
        }
        req.getRequestDispatcher("/WEB-INF/views/shared/catalog.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String p = ViewSupport.current(req);
        if (!CatalogService.LABELS.containsKey(kind(p))) return false;
        if (!ViewSupport.validCsrf(req)) {
            resp.sendError(403, "Phiên biểu mẫu đã hết hạn. Tải lại trang.");
            return true;
        }
        User u = (User) req.getSession().getAttribute("user");
        if (u.getRole() == Role.ADMIN && !java.util.Set.of("notifications", "topics").contains(p)) {
            resp.sendError(403);
            return true;
        }
        try {
            String action = req.getParameter("action");
            CatalogService store = CatalogService.getInstance();
            if ("save".equals(action)) {
                String v = req.getParameter("version");
                com.smartstudy.service.CourseService cs =
                        com.smartstudy.service.CourseService.getInstance();
                var placement =
                        u.getRole() == Role.TEACHER
                                ? cs.validate(
                                        u,
                                        req.getParameter("chapter"),
                                        Integer.parseInt(req.getParameter("order")),
                                        req.getParameter("resourceUrl"))
                                : null;
                CatalogEntry saved =
                        store.save(
                                u,
                                kind(p),
                                req.getParameter("id"),
                                req.getParameter("title"),
                                req.getParameter("body"),
                                req.getParameter("topic"),
                                req.getParameter("status"),
                                v == null ? 0 : Integer.parseInt(v));
                if (placement != null) cs.place(saved.id(), placement);
            } else if ("archive".equals(action) || "restore".equals(action)) {
                CatalogEntry e = store.get(req.getParameter("id"));
                if (e == null || !e.kind().equals(kind(p)))
                    throw new IllegalArgumentException("Không tìm thấy bản ghi.");
                store.save(
                        u,
                        kind(p),
                        e.id(),
                        e.title(),
                        e.body(),
                        e.topic(),
                        "archive".equals(action) ? "archived" : "draft",
                        Integer.parseInt(req.getParameter("version")));
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            req.getSession().setAttribute("flash", "Đã lưu thay đổi.");
            resp.sendRedirect(req.getContextPath() + "/" + ViewSupport.role(u) + "/" + p);
        } catch (IllegalArgumentException ex) {
            req.setAttribute("formError", ex.getMessage());
            req.setAttribute("editing", true);
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
