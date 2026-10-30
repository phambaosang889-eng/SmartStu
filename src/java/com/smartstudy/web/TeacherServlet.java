package com.smartstudy.web;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/teacher/*")
public class TeacherServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (TutorController.get(req, resp)
                || AIController.get(req, resp)
                || AccountController.get(req, resp)
                || CourseController.get(req, resp)
                || ManagementController.get(req, resp)
                || DashboardController.get(req, resp)
                || QuizController.get(req, resp)
                || EssayController.get(req, resp)
                || QuestionController.get(req, resp)) return;
        if (ViewSupport.current(req).equals("ai-settings")) {
            req.getRequestDispatcher("/WEB-INF/views/teacher/ai-settings.jsp").forward(req, resp);
            return;
        }
        resp.sendError(404);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!TutorController.post(req, resp)
                && !AIController.post(req, resp)
                && !AccountController.post(req, resp)
                && !CourseController.post(req, resp)
                && !ManagementController.post(req, resp)
                && !QuizController.post(req, resp)
                && !EssayController.post(req, resp)
                && !QuestionController.post(req, resp)) resp.sendError(400);
    }
}
