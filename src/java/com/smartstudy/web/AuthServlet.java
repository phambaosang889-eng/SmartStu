package com.smartstudy.web;

import author.Role;
import author.User;
import author.UserStore;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/app", "/home", "/login", "/register", "/logout"})
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "/app" -> {
                req.setAttribute("nextUrl", req.getContextPath() + "/home");
                req.getRequestDispatcher("/WEB-INF/views/common/splash.jsp").forward(req, resp);
            }
            case "/home" ->
                    req.getRequestDispatcher("/WEB-INF/views/common/home.jsp").forward(req, resp);
            case "/login" -> {
                HttpSession existing = req.getSession(false);
                if (existing != null) {
                    req.setAttribute("authNotice", existing.getAttribute("authNotice"));
                    existing.removeAttribute("authNotice");
                }
                req.getRequestDispatcher("/WEB-INF/views/common/login.jsp").forward(req, resp);
            }
            case "/register" ->
                    req.getRequestDispatcher("/WEB-INF/views/common/register.jsp")
                            .forward(req, resp);
            case "/logout" -> {
                if (!ViewSupport.validCsrf(req)) {
                    resp.sendError(403);
                    return;
                }
                HttpSession s = req.getSession(false);
                if (s != null) s.invalidate();
                resp.sendRedirect(req.getContextPath() + "/login?logout=1");
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        if (!java.util.Set.of("/login", "/register").contains(req.getServletPath())
                || !ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return;
        }
        if ("/register".equals(req.getServletPath())) {
            try {
                String selectedRole = req.getParameter("role");
                if (selectedRole != null && !"STUDENT".equals(selectedRole))
                    throw new IllegalArgumentException(
                            "Đăng ký trực tuyến chỉ dành cho học sinh. Tài khoản giáo viên do quản"
                                + " trị viên cấp.");
                UserStore.registerStudent(
                        req.getParameter("name"),
                        req.getParameter("email"),
                        req.getParameter("password"),
                        req.getParameter("confirmPassword"));
                req.getSession()
                        .setAttribute(
                                "authNotice",
                                "Tạo tài khoản thành công. Chọn Học sinh và đăng nhập bằng tài"
                                    + " khoản vừa tạo.");
                resp.sendRedirect(req.getContextPath() + "/login");
            } catch (IllegalArgumentException e) {
                req.setAttribute("registrationError", e.getMessage());
                req.setAttribute("registrationName", req.getParameter("name"));
                req.setAttribute("registrationEmail", req.getParameter("email"));
                resp.setStatus(400);
                req.getRequestDispatcher("/WEB-INF/views/common/register.jsp").forward(req, resp);
            }
            return;
        }

        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String roleValue = req.getParameter("role");
        Role role;
        try {
            role = Role.valueOf(roleValue == null ? "" : roleValue.toUpperCase());
        } catch (IllegalArgumentException ex) {
            resp.sendRedirect(req.getContextPath() + "/login?error=role");
            return;
        }

        User user = UserStore.authenticate(email, password, role);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login?error=1");
            return;
        }

        HttpSession old = req.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);
        session.setAttribute("role", user.getRole().name());

        resp.sendRedirect(req.getContextPath() + user.getRole().getHomePath());
    }
}
