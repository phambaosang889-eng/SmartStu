package com.smartstudy.web;

import author.*;
import com.smartstudy.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class AccountController {
    private AccountController() {}

    private static boolean handles(String p) {
        return Set.of("users", "teachers", "students", "profile", "audit").contains(p);
    }

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String p = ViewSupport.current(req);
        if (!handles(p)) return false;
        User u = (User) req.getSession().getAttribute("user");
        if (!p.equals("profile")
                && u.getRole() != Role.ADMIN
                && !(u.getRole() == Role.TEACHER && p.equals("students"))) {
            resp.sendError(403);
            return true;
        }
        var allowed = CourseService.getInstance().students(u);
        List<UserStore.Account> accounts =
                UserStore.list().stream()
                        .filter(
                                a ->
                                        u.getRole() == Role.ADMIN
                                                || allowed.contains(a.user().getEmail()))
                        .filter(a -> !p.equals("teachers") || a.user().getRole() == Role.TEACHER)
                        .toList();
        req.setAttribute("accounts", accounts);
        String email = req.getParameter("email");
        if (email != null) {
            var a =
                    accounts.stream()
                            .filter(x -> x.user().getEmail().equals(email))
                            .findFirst()
                            .orElse(null);
            if (a == null && req.getAttribute("accountError") == null) {
                resp.sendError(404);
                return true;
            }
            if (a != null) req.setAttribute("account", a);
        }
        req.getRequestDispatcher("/WEB-INF/views/shared/accounts.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String p = ViewSupport.current(req);
        if (!handles(p)) return false;
        if (!ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return true;
        }
        User u = (User) req.getSession().getAttribute("user");
        try {
            String action = req.getParameter("action");
            if (p.equals("profile") && "profile".equals(action))
                req.getSession()
                        .setAttribute("user", UserStore.profile(u, req.getParameter("name")));
            else if (p.equals("profile") && "password".equals(action)) {
                if (!Objects.equals(
                        req.getParameter("newPassword"), req.getParameter("confirmPassword")))
                    throw new IllegalArgumentException("Mật khẩu xác nhận chưa khớp.");
                UserStore.password(
                        u, req.getParameter("currentPassword"), req.getParameter("newPassword"));
                req.changeSessionId();
                req.getSession().removeAttribute("csrf");
            } else if ((p.equals("users") || p.equals("teachers"))
                    && u.getRole() == Role.ADMIN
                    && "save-account".equals(action)) {
                String selectedRole = req.getParameter("role");
                if (selectedRole == null
                        || !java.util.List.of("STUDENT", "TEACHER", "ADMIN").contains(selectedRole))
                    throw new IllegalArgumentException("Vai trò không hợp lệ.");
                UserStore.save(
                        u,
                        req.getParameter("email"),
                        req.getParameter("name"),
                        Role.valueOf(selectedRole),
                        "locked".equals(req.getParameter("status")),
                        Integer.parseInt(req.getParameter("version")),
                        req.getParameter("password"));
            } else {
                resp.sendError(403);
                return true;
            }
            req.getSession()
                    .setAttribute(
                            "flash",
                            "Đã cập nhật. Trạng thái và vai trò được kiểm tra lại ở mỗi yêu cầu"
                                + " truy cập.");
            resp.sendRedirect(req.getContextPath() + "/" + ViewSupport.role(u) + "/" + p);
        } catch (IllegalArgumentException e) {
            req.setAttribute(
                    "accountError",
                    e instanceof NumberFormatException
                            ? "Phiên bản không hợp lệ."
                            : e.getMessage());
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
