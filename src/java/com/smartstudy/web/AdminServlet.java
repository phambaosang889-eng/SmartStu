package com.smartstudy.web;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (TutorController.get(req, resp)
                || AIController.get(req, resp)
                || AccountController.get(req, resp)
                || CourseController.get(req, resp)
                || ManagementController.get(req, resp)
                || DashboardController.get(req, resp)
                || QuizController.get(req, resp)) return;

        String page = req.getPathInfo();

        if (page == null || page.equals("/")) {
            page = "/dashboard";
        }

        String view =
                switch (page) {
                    case "/dashboard" -> "dashboard";
                    case "/courses" -> "courses";
                    case "/lesson" -> "lesson";
                    case "/quiz" -> "quiz";
                    case "/flashcards" -> "flashcards";
                    case "/progress" -> "progress";
                    case "/users" -> "users";
                    case "/teachers" -> "teachers";
                    case "/content" -> "content";
                    case "/reports" -> "reports";
                    case "/settings" -> "settings";
                    default -> null;
                };

        if (view == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/admin/" + view + ".jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if (!TutorController.post(req, resp)
                && !AIController.post(req, resp)
                && !AccountController.post(req, resp)
                && !CourseController.post(req, resp)
                && !ManagementController.post(req, resp)
                && !QuizController.post(req, resp)) resp.sendError(400, "Thao tác không hợp lệ.");
    }
}
