package com.smartstudy.web;

import author.*;
import com.smartstudy.service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;

@WebServlet("/teacher/lesson-video")
@MultipartConfig(fileSizeThreshold = 1048576, maxFileSize = 209715200, maxRequestSize = 210763776)
public final class LessonVideoServlet extends HttpServlet {
    private final LessonVideoService videos = new LessonVideoService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null || user.getRole() != Role.TEACHER) {
            resp.sendError(403);
            return;
        }
        String lesson = null;
        try {
            if (!ViewSupport.validCsrf(req)) {
                resp.sendError(403);
                return;
            }
            lesson = req.getParameter("lesson");
            videos.requireOwner(user, lesson);
            String action = req.getParameter("action");
            if ("link-video".equals(action))
                videos.link(
                        user,
                        lesson,
                        req.getParameter("title"),
                        req.getParameter("url"),
                        req.getParameter("source"));
            else if ("upload-video".equals(action)) {
                Part part = req.getPart("video");
                if (part == null || part.getSize() == 0)
                    throw new IllegalArgumentException("Chọn file video MP4 hoặc WebM để tải lên.");
                if (!"on".equals(req.getParameter("rights")))
                    throw new IllegalArgumentException(
                            "Xác nhận quyền sử dụng video trước khi tải lên.");
                try (var input = part.getInputStream()) {
                    videos.upload(
                            user,
                            lesson,
                            req.getParameter("title"),
                            part.getSubmittedFileName(),
                            part.getSize(),
                            input);
                } finally {
                    part.delete();
                }
            } else if ("archive-video".equals(action)) {
                var v = videos.readable(user, req.getParameter("id"));
                if (v == null || !v.lesson().equals(lesson))
                    throw new IllegalArgumentException("Video không thuộc bài học này.");
                videos.archive(user, v.id());
            } else throw new IllegalArgumentException("Thao tác video không hợp lệ.");
            req.getSession().setAttribute("flash", "Đã cập nhật video bài giảng.");
            resp.sendRedirect(
                    req.getContextPath()
                            + "/teacher/lesson?mode=edit&id="
                            + java.net.URLEncoder.encode(
                                    lesson, java.nio.charset.StandardCharsets.UTF_8));
        } catch (IllegalStateException e) {
            resp.setStatus(413);
            req.setAttribute(
                    "videoError",
                    "Video vượt dung lượng 200 MB hoặc biểu mẫu tải lên không hợp lệ.");
            req.getRequestDispatcher("/WEB-INF/views/common/video-error.jsp").forward(req, resp);
        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            req.setAttribute("videoError", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/common/video-error.jsp").forward(req, resp);
        } catch (IOException e) {
            getServletContext().log("Video storage unavailable: " + e.getClass().getSimpleName());
            resp.setStatus(503);
            req.setAttribute(
                    "videoError",
                    "Không lưu được file video. Kiểm tra dung lượng và thư mục lưu trên máy chủ,"
                        + " rồi thử lại.");
            req.getRequestDispatcher("/WEB-INF/views/common/video-error.jsp").forward(req, resp);
        }
    }
}
