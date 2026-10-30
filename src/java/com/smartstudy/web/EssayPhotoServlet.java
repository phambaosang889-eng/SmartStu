package com.smartstudy.web;

import author.*;
import com.smartstudy.service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {"/student/essay-photo", "/teacher/essay-photo", "/media/essay-photo"})
@MultipartConfig(fileSizeThreshold = 1048576, maxFileSize = 8388608, maxRequestSize = 9437184)
public final class EssayPhotoServlet extends HttpServlet {
    private final EssayPhotoService photos = new EssayPhotoService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!req.getServletPath().equals("/media/essay-photo")) {
            resp.sendError(405);
            return;
        }
        User user = (User) req.getSession().getAttribute("user");
        var photo = photos.readable(user, req.getParameter("id"));
        if (photo == null) {
            resp.sendError(404);
            return;
        }
        byte[] bytes = photos.bytes(user, photo.id());
        if (bytes == null) {
            resp.sendError(404);
            return;
        }
        resp.setHeader("Cache-Control", "private, no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Disposition", "inline; filename=\"bai-lam.jpg\"");
        resp.setContentType("image/jpeg");
        resp.setContentLength(bytes.length);
        resp.getOutputStream().write(bytes);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        User u = (User) req.getSession().getAttribute("user");
        if (u == null
                || !(u.getRole() == Role.STUDENT || u.getRole() == Role.TEACHER)
                || req.getServletPath().startsWith("/media/")) {
            resp.sendError(403);
            return;
        }
        String destination = req.getContextPath() + "/" + ViewSupport.role(u) + "/essays";
        try {
            if (!ViewSupport.validCsrf(req)) {
                resp.sendError(403);
                return;
            }
            String action = req.getParameter("action"), assignment = req.getParameter("assignment");
            var existing = photos.readable(u, req.getParameter("photo"));
            if (existing != null) {
                destination +=
                        existing.submission() == null
                                ? "?id=" + encode(existing.assignment())
                                : "?submission=" + encode(existing.submission());
            } else if (assignment != null) destination += "?id=" + encode(assignment);
            if (existing != null
                    && u.getRole() == Role.STUDENT
                    && existing.submission() == null
                    && req.getParameter("draftAnswer") != null)
                EssayWorkflow.getInstance()
                        .draft(u, existing.assignment(), req.getParameter("draftAnswer"));
            if ("upload".equals(action)) {
                if (u.getRole() != Role.STUDENT)
                    throw new IllegalArgumentException("Chỉ học sinh được tải bài làm lên.");
                Part part = req.getPart("photoFile");
                if (part == null)
                    throw new IllegalArgumentException(
                            "Chọn ảnh hoặc chụp bài làm trước khi tải lên.");
                try (var input = part.getInputStream()) {
                    photos.upload(u, assignment, input);
                } finally {
                    part.delete();
                }
                // Preserve current text while the browser navigates after the photo POST.
                EssayWorkflow.getInstance()
                        .draft(
                                u,
                                assignment,
                                req.getParameter("draftAnswer") == null
                                        ? ""
                                        : req.getParameter("draftAnswer"));
            } else if ("scan".equals(action)) {
                if (!"yes".equals(req.getParameter("aiConsent")))
                    throw new IllegalArgumentException(
                            "Xác nhận gửi ảnh bài làm đến Gemini để đọc nội dung.");
                photos.scan(u, req.getParameter("photo"));
            } else if ("remove".equals(action)) photos.remove(u, req.getParameter("photo"));
            else throw new IllegalArgumentException("Thao tác ảnh không hợp lệ.");
            req.getSession()
                    .setAttribute(
                            "flash",
                            "scan".equals(action)
                                    ? "Đã đọc ảnh. Hãy đối chiếu nội dung nhận dạng với bài viết"
                                          + " trước khi sử dụng."
                                    : "Đã cập nhật ảnh bài làm.");
            resp.sendRedirect(destination);
        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            showError(req, resp, e.getMessage(), destination);
        } catch (IllegalStateException e) {
            resp.setStatus(413);
            showError(
                    req,
                    resp,
                    "Ảnh vượt quá 8 MB. Chọn ảnh nhỏ hơn hoặc chụp lại vùng bài làm.",
                    destination);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void showError(
            HttpServletRequest req, HttpServletResponse resp, String error, String destination)
            throws IOException, ServletException {
        req.setAttribute("photoError", error);
        req.setAttribute("photoBack", destination);
        req.getRequestDispatcher("/WEB-INF/views/common/essay-photo-error.jsp").forward(req, resp);
    }
}
