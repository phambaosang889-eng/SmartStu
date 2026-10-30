package com.smartstudy.web;

import author.*;
import com.smartstudy.model.*;
import com.smartstudy.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class QuizController {
    private QuizController() {}

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("quiz")) return false;
        User u = (User) req.getSession().getAttribute("user");
        QuizService s = QuizService.getInstance();
        if (u.getRole() == Role.ADMIN && "edit".equals(req.getParameter("mode"))) {
            resp.sendError(403);
            return true;
        }
        req.setAttribute("quizzes", s.list(u));
        req.setAttribute("quizQuestions", s.questions(u));
        req.setAttribute("quizHistory", s.history(u));
        String id = req.getParameter("id");
        if (id != null && !id.isBlank()) {
            QuizDefinition q = s.get(id, u);
            if (q == null) {
                resp.sendError(404);
                return true;
            }
            req.setAttribute("quiz", q);
        }
        String token = req.getParameter("run");
        if (token != null) {
            QuizService.Run run = s.run(token, u);
            if (run == null) {
                resp.sendError(404);
                return true;
            }
            req.setAttribute("quizRun", run);
            req.setAttribute("quizResult", s.result(token, u));
        }
        req.getRequestDispatcher("/WEB-INF/views/shared/quiz.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("quiz")) return false;
        if (!ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return true;
        }
        User u = (User) req.getSession().getAttribute("user");
        QuizService s = QuizService.getInstance();
        String dest = req.getContextPath() + "/" + ViewSupport.role(u) + "/quiz";
        if (u.getRole() == Role.ADMIN) {
            resp.sendError(403);
            return true;
        }
        try {
            String action = req.getParameter("action");
            if ("start".equals(action)) {
                dest += "?run=" + s.start(req.getParameter("id"), u).token();
            } else if ("submit".equals(action)) {
                s.submit(req.getParameter("token"), u, req.getParameterMap());
                dest += "?run=" + req.getParameter("token");
            } else if ("copy".equals(action)) {
                dest += "?id=" + s.copy(req.getParameter("id"), u).id();
                req.getSession().setAttribute("flash", "Đã tạo bản sao dưới dạng bản nháp.");
            } else if ("save-quiz".equals(action)) {
                String[] ids = req.getParameterValues("question");
                List<String> selected =
                        ids == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(ids));
                selected.sort(
                        java.util.Comparator.comparingInt(
                                key -> {
                                    String value = req.getParameter("order_" + key);
                                    int order = value == null ? 1 : Integer.parseInt(value);
                                    if (order < 1 || order > 999)
                                        throw new IllegalArgumentException(
                                                "Thứ tự câu hỏi từ 1–999.");
                                    return order;
                                }));
                String due = req.getParameter("due");
                java.time.LocalDateTime date = null;
                try {
                    if (due != null && !due.isBlank()) date = java.time.LocalDateTime.parse(due);
                } catch (java.time.format.DateTimeParseException e) {
                    throw new IllegalArgumentException("Thời hạn không hợp lệ.");
                }
                QuizDefinition q =
                        s.save(
                                u,
                                req.getParameter("id"),
                                req.getParameter("title"),
                                req.getParameter("description"),
                                Integer.parseInt(req.getParameter("minutes")),
                                req.getParameter("status"),
                                selected,
                                Integer.parseInt(req.getParameter("version")),
                                date,
                                Integer.parseInt(req.getParameter("maxAttempts")),
                                Integer.parseInt(req.getParameter("maxScore")),
                                req.getParameter("course"));
                dest += "?id=" + q.id();
                req.getSession()
                        .setAttribute(
                                "flash",
                                "Đã lưu quiz. Học sinh chỉ thấy quiz đã phát hành trong khóa học đã"
                                    + " tham gia.");
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            resp.sendRedirect(dest);
        } catch (IllegalArgumentException ex) {
            req.setAttribute("quizError", ex.getMessage());
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
