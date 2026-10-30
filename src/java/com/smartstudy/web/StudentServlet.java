package com.smartstudy.web;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/student/*")
public class StudentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (TutorController.get(req, resp)
                || AccountController.get(req, resp)
                || CourseController.get(req, resp)
                || DashboardController.get(req, resp)
                || LearningController.get(req, resp)
                || QuizController.get(req, resp)
                || EssayController.get(req, resp)) return;
        String p = ViewSupport.current(req);
        if (!java.util.Set.of("formulas", "mindmap").contains(p)) {
            resp.sendError(404);
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/student/" + p + ".jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!TutorController.post(req, resp)
                && !AccountController.post(req, resp)
                && !CourseController.post(req, resp)
                && !LearningController.post(req, resp)
                && !QuizController.post(req, resp)
                && !EssayController.post(req, resp)) resp.sendError(400);
    }
}
