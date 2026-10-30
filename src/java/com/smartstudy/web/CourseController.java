package com.smartstudy.web;

import author.*;
import com.smartstudy.service.*;
import com.smartstudy.model.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public final class CourseController {
    private CourseController() {}

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("courses")) return false;
        User u = (User) req.getSession().getAttribute("user");
        var cs = CourseService.getInstance();
        req.setAttribute("courses", cs.courses(u));
        String id = req.getParameter("id");
        if (id != null && !id.isBlank()) {
            CatalogEntry c =
                    cs.courses(u).stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
            if (c == null) {
                resp.sendError(404);
                return true;
            }
            req.setAttribute("course", c);
            req.setAttribute("chapters", cs.chapters(id));
            req.setAttribute("courseLessons", cs.lessons(id));
        }
        if (u.getRole() != Role.TEACHER && "edit".equals(req.getParameter("mode"))) {
            resp.sendError(403);
            return true;
        }
        req.getRequestDispatcher("/WEB-INF/views/shared/courses.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("courses")) return false;
        if (!ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return true;
        }
        User u = (User) req.getSession().getAttribute("user");
        var cs = CourseService.getInstance();
        String id = req.getParameter("id");
        String dest = req.getContextPath() + "/" + ViewSupport.role(u) + "/courses";
        try {
            String action = req.getParameter("action");
            if ("enroll".equals(action) || "leave".equals(action)) {
                cs.enroll(u, id, "enroll".equals(action));
            } else if ("save-course".equals(action)) {
                String cover = cs.validateCover(req.getParameter("cover"));
                var c =
                        CatalogService.getInstance()
                                .save(
                                        u,
                                        "courses",
                                        id,
                                        req.getParameter("title"),
                                        req.getParameter("body"),
                                        req.getParameter("topic"),
                                        req.getParameter("status"),
                                        Integer.parseInt(req.getParameter("version")));
                id = c.id();
                cs.cover(u, id, cover);
                AuditService.record(u.getEmail(), "Lưu khóa học", id + " · " + c.status());
            } else if ("moderate".equals(action)) {
                CatalogService.getInstance()
                        .moderate(
                                u,
                                id,
                                req.getParameter("status"),
                                req.getParameter("reason"),
                                Integer.parseInt(req.getParameter("version")));
            } else if ("save-chapter".equals(action)) {
                cs.chapter(
                        u,
                        id,
                        req.getParameter("chapter"),
                        req.getParameter("title"),
                        Integer.parseInt(req.getParameter("order")));
            } else if ("delete-chapter".equals(action)) {
                cs.removeChapter(u, id, req.getParameter("chapter"));
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            req.getSession()
                    .setAttribute(
                            "flash",
                            "Đã cập nhật khóa học. Nội dung chỉ đến học sinh khi khóa học được"
                                + " duyệt và xuất bản.");
            resp.sendRedirect(dest + (id == null || id.isBlank() ? "" : "?id=" + id));
        } catch (IllegalArgumentException ex) {
            req.setAttribute(
                    "courseError",
                    ex instanceof NumberFormatException
                            ? "Thứ tự hoặc phiên bản không hợp lệ."
                            : ex.getMessage());
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
