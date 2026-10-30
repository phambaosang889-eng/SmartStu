package com.smartstudy.web;

import author.User;
import com.smartstudy.model.view.LayoutModel;
import java.io.IOException;
import java.time.Year;
import java.time.ZoneId;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

/** Prepares shared view data at the controller boundary, once per forward. */
@WebFilter(urlPatterns = "/*", dispatcherTypes = DispatcherType.FORWARD)
public final class LayoutFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        User user =
                req.getSession(false) == null
                        ? null
                        : (User) req.getSession(false).getAttribute("user");
        if (user != null
                && req.getServletPath().startsWith("/WEB-INF/views/")
                && !req.getServletPath().startsWith("/WEB-INF/views/common/")) {
            String current = ViewSupport.current(req);
            var menu = ViewSupport.menu(user);
            var navigation =
                    menu.entrySet().stream()
                            .map(
                                    e ->
                                            new LayoutModel.NavigationItem(
                                                    e.getKey(),
                                                    e.getValue(),
                                                    ViewSupport.icon(e.getKey()),
                                                    current.equals(e.getKey())))
                            .toList();
            Object flash = req.getSession().getAttribute("flash");
            req.getSession().removeAttribute("flash");
            req.setAttribute(
                    "layout",
                    new LayoutModel(
                            user,
                            req.getContextPath() + "/" + ViewSupport.role(user),
                            current,
                            menu.getOrDefault(current, "SmartStu"),
                            ViewSupport.roleLabel(user),
                            navigation,
                            flash == null ? null : flash.toString(),
                            Year.now(ZoneId.of("Asia/Bangkok")).getValue()));
        }
        chain.doFilter(request, response);
    }
}
