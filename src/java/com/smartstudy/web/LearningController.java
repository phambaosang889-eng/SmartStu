package com.smartstudy.web;

import author.User;
import com.smartstudy.model.CatalogEntry;
import com.smartstudy.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class LearningController {
    private LearningController() {}

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String p = ViewSupport.current(req);
        if (!Set.of("lesson", "flashcards", "resources", "notifications").contains(p)) return false;
        User user = (User) req.getSession().getAttribute("user");
        String kind = p;
        List<CatalogEntry> items = CourseService.getInstance().contents(user, kind);
        if (p.equals("notifications")) {
            var list = new ArrayList<>(items);
            CatalogService.getInstance().published(kind).stream()
                    .filter(
                            e ->
                                    author.UserStore.account(e.owner()) != null
                                            && author.UserStore.account(e.owner()).user().getRole()
                                                    == author.Role.ADMIN)
                    .forEach(list::add);
            items = List.copyOf(list);
        }
        String course = req.getParameter("course");
        if (course != null)
            items =
                    items.stream()
                            .filter(
                                    e ->
                                            CourseService.getInstance()
                                                    .courseOf(e.id())
                                                    .equals(course))
                            .toList();
        req.setAttribute("learningItems", items);
        req.setAttribute("completed", LearningService.getInstance().completed(user.getEmail()));
        req.setAttribute("known", LearningService.getInstance().known(user.getEmail()));
        String id = req.getParameter("id");
        CatalogEntry entry = null;
        if (id != null) {
            entry = items.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
            if (entry == null) {
                resp.sendError(404);
                return true;
            }
            if (p.equals("lesson")) LearningService.getInstance().read(user.getEmail(), entry.id());
        }
        if (p.equals("flashcards") && !items.isEmpty()) {
            if ("1".equals(req.getParameter("shuffle")))
                entry =
                        items.get(
                                java.util.concurrent.ThreadLocalRandom.current()
                                        .nextInt(items.size()));
            if (entry == null) entry = items.get(0);
        }
        if (entry != null) {
            if (p.equals("lesson")) {
                String within = CourseService.getInstance().courseOf(entry.id());
                items =
                        items.stream()
                                .filter(
                                        e ->
                                                CourseService.getInstance()
                                                        .courseOf(e.id())
                                                        .equals(within))
                                .toList();
                req.setAttribute("learningItems", items);
            }
            int index = items.indexOf(entry);
            req.setAttribute("learningEntry", entry);
            req.setAttribute("itemIndex", index);
            req.setAttribute("previous", items.get((index + items.size() - 1) % items.size()));
            req.setAttribute("next", items.get((index + 1) % items.size()));
        }
        if (entry != null && p.equals("lesson"))
            req.setAttribute("lessonVideos", new LessonVideoService().list(user, entry.id()));
        req.getRequestDispatcher("/WEB-INF/views/shared/learning.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String action = req.getParameter("action");
        if (!Set.of("complete", "mark-card", "study-pulse").contains(action == null ? "" : action))
            return false;
        if (!ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return true;
        }
        if (action.equals("mark-card")
                && !"known".equals(req.getParameter("state"))
                && !"practice".equals(req.getParameter("state"))) {
            resp.sendError(400);
            return true;
        }
        User user = (User) req.getSession().getAttribute("user");
        CatalogEntry entry = CatalogService.getInstance().get(req.getParameter("id"));
        if (entry == null
                || !CourseService.getInstance().canLearn(user, entry.id())
                || !entry.status().equals("published")
                || !entry.kind().equals(action.equals("mark-card") ? "flashcards" : "lesson")) {
            resp.sendError(404);
            return true;
        }
        if (action.equals("study-pulse")) {
            LearningService.getInstance().pulse(user.getEmail(), entry.id());
            resp.setStatus(204);
            return true;
        }
        if (action.equals("complete")) {
            LearningService.getInstance().complete(user.getEmail(), entry.id(), entry.title());
            req.getSession().setAttribute("flash", "Đã ghi nhận hoàn thành bài học.");
        } else
            LearningService.getInstance()
                    .mark(user.getEmail(), entry.id(), "known".equals(req.getParameter("state")));
        resp.sendRedirect(req.getContextPath() + "/student/" + entry.kind() + "?id=" + entry.id());
        return true;
    }
}
