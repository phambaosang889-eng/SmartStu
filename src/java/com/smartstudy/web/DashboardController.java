package com.smartstudy.web;

import author.*;
import com.smartstudy.service.*;
import com.smartstudy.model.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class DashboardController {
    public record Metric(String label, String value, String note) {}

    private DashboardController() {}

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String p = ViewSupport.current(req);
        if (!Set.of("dashboard", "progress", "reports").contains(p)) return false;
        User u = (User) req.getSession().getAttribute("user");
        boolean student = u.getRole() == Role.STUDENT, admin = u.getRole() == Role.ADMIN;
        var cs = CourseService.getInstance();
        var ls = LearningService.getInstance();
        var catalog = CatalogService.getInstance();
        var lessons = cs.contents(u, "lesson");
        var history = QuizService.getInstance().history(u);
        var essays = EssayWorkflow.getInstance().monitoring(u);
        java.time.LocalDate from = null, to = null;
        try {
            if (req.getParameter("from") != null && !req.getParameter("from").isBlank())
                from = java.time.LocalDate.parse(req.getParameter("from"));
            if (req.getParameter("to") != null && !req.getParameter("to").isBlank())
                to = java.time.LocalDate.parse(req.getParameter("to"));
            if (from != null && to != null && from.isAfter(to))
                throw new IllegalArgumentException();
        } catch (Exception e) {
            req.setAttribute(
                    "reportError",
                    "Khoảng ngày không hợp lệ. Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            resp.setStatus(400);
        }
        var rows =
                req.getAttribute("reportError") != null
                        ? List.<ReportingService.CourseRow>of()
                        : ReportingService.courses(
                                u,
                                req.getParameter("course"),
                                admin ? req.getParameter("teacher") : null,
                                from,
                                to);
        if ("csv".equals(req.getParameter("export"))
                && !student
                && req.getAttribute("reportError") == null) {
            resp.setContentType("text/csv;charset=UTF-8");
            resp.setHeader(
                    "Content-Disposition", "attachment; filename=smartstu-course-report.csv");
            resp.getWriter().write(ReportingService.csv(rows));
            return true;
        }
        var metrics = DashboardService.metrics(u, lessons, history, essays);
        req.setAttribute("metrics", metrics);
        req.setAttribute("lessons", lessons);
        req.setAttribute("completed", ls.completed(u.getEmail()));
        req.setAttribute("activities", ReportingService.activities(u).stream().limit(6).toList());
        req.setAttribute("courseRows", rows);
        req.setAttribute("chart", DashboardService.bars(ReportingService.daily(u, 7)));
        req.setAttribute("topics", ReportingService.topics(u));
        req.setAttribute("quizHistory", history);
        req.setAttribute("growthCharts", DashboardService.growth(u));
        req.setAttribute("hasLearningContent", !lessons.isEmpty());
        req.setAttribute(
                "nextLessons", DashboardService.nextLessons(lessons, ls.completed(u.getEmail())));
        req.setAttribute("reportCourses", cs.courses(u));
        req.setAttribute(
                "reportTeachers",
                admin
                        ? UserStore.list().stream()
                                .filter(a -> a.user().getRole() == Role.TEACHER)
                                .toList()
                        : List.of());
        req.getRequestDispatcher("/WEB-INF/views/shared/dashboard.jsp").forward(req, resp);
        return true;
    }
}
