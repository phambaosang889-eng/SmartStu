package com.smartstudy.web;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {

    private boolean systemPage(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (!java.util.Set.of(
                        "dashboard",
                        "users",
                        "notifications",
                        "ai-settings",
                        "audit",
                        "settings",
                        "profile")
                .contains(ViewSupport.current(req))) {
            resp.sendError(403);
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!systemPage(req, resp)) return;

        if (AIController.get(req, resp)
                || AccountController.get(req, resp)
                || ManagementController.get(req, resp)
                || DashboardController.get(req, resp)) return;
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!systemPage(req, resp)) return;
        req.setCharacterEncoding("UTF-8");
        if (!AIController.post(req, resp)
                && !AccountController.post(req, resp)
                && !ManagementController.post(req, resp))
            resp.sendError(400, "Thao tác không hợp lệ.");
    }
}
