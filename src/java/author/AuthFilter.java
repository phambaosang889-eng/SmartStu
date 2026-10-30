/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package author;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("Referrer-Policy", "same-origin");
        String ctx = req.getContextPath();
        // Use the container's decoded and normalized mapping, not the raw URI.
        // Encoded names, path parameters and dot segments must not bypass role checks.
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());

        // 1) Công khai
        if (path.isEmpty()
                || path.equals("/")
                || path.equals("/app")
                || path.equals("/home")
                || path.equals("/login")
                || path.equals("/register")
                || path.equals("/logout")
                || path.startsWith("/assets/")) {
            if (java.util.Set.of("/login", "/register", "/logout").contains(path))
                resp.setHeader("Cache-Control", "no-store");
            chain.doFilter(request, response);
            return;
        }

        // 2) Chưa đăng nhập
        HttpSession session = req.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");
        if (user == null) {
            resp.sendRedirect(ctx + "/login");
            return;
        }
        User refreshed = UserStore.active(user.getEmail());
        if (refreshed == null || refreshed.getRole() != user.getRole()) {
            session.invalidate();
            resp.sendRedirect(ctx + "/login?error=session");
            return;
        }
        user = refreshed;
        session.setAttribute("user", user);
        SessionActivity.touch(session.getId(), user.getEmail());

        // 3) Đúng vai trò với tiền tố URL không?
        Role need = null;
        if (path.equals("/student") || path.startsWith("/student/")) need = Role.STUDENT;
        else if (path.equals("/teacher") || path.startsWith("/teacher/")) need = Role.TEACHER;
        else if (path.equals("/admin") || path.startsWith("/admin/")) need = Role.ADMIN;

        if (need != null && user.getRole() != need) {
            resp.setStatus(403);
            req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, resp);
            return;
        }

        resp.setHeader("Cache-Control", "no-store");
        chain.doFilter(request, response);
    }
}
