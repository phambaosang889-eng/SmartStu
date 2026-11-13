package com.smartstudy.web;

import author.*;
import com.smartstudy.service.EssayWorkflow;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class EssayController {

    private EssayController() {
    }

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("essays")) {
            return false;
        }
        User u = (User) req.getSession().getAttribute("user");
        EssayWorkflow s = EssayWorkflow.getInstance();
        req.setAttribute("assignments", s.assignments(u));
        req.setAttribute("submissions", s.submissions(u));
        String id = req.getParameter("id");
        if (id != null && !id.isBlank()) {
            var a = s.assignment(id, u);
            if (a == null) {
                resp.sendError(404);
                return true;
            }
            req.setAttribute("assignment", a);
            req.setAttribute("draft", s.draft(u, id));
        }
        String sid = req.getParameter("submission");
        if (sid != null) {
            var sub = s.submission(sid, u);
            if (sub == null) {
                resp.sendError(404);
                return true;
            }
            req.setAttribute("submission", sub);
        }
        var photos = new com.smartstudy.service.EssayPhotoService();
        if (req.getAttribute("submission") != null) {
            req.setAttribute("essayPhoto", photos.submitted(u, sid));
        } else if (req.getAttribute("assignment") != null && u.getRole() == Role.STUDENT) {
            req.setAttribute("essayPhoto", photos.draft(u, id));
        }
        if (req.getSession().getAttribute("essayToken") == null) {
            req.getSession().setAttribute("essayToken", UUID.randomUUID().toString());
        }
        req.getRequestDispatcher("/WEB-INF/views/shared/essays.jsp").forward(req, resp);
        return true;
    }

    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!ViewSupport.current(req).equals("essays")) {
            return false;
        }
        if (!ViewSupport.validCsrf(req)) {
            resp.sendError(403);
            return true;
        }
        User u = (User) req.getSession().getAttribute("user");
        EssayWorkflow s = EssayWorkflow.getInstance();
        String dest = req.getContextPath() + "/" + ViewSupport.role(u) + "/essays";
        try {
            String action = req.getParameter("action");
            if ("save-assignment".equals(action)) {
                /*
                            if ("generate-practice".equals(action)) {
                if (u.getRole() != Role.STUDENT) {
                    resp.sendError(403);
                    return true;
                }
                String topic = switch (Objects.toString(req.getParameter("topic"), "")) {
                    case "motion" -> "chuyển động thẳng";
                    case "newton" -> "các định luật Newton";
                    case "energy" -> "công và năng lượng";
                    default -> throw new IllegalArgumentException("Chủ đề không hợp lệ.");
                };
                String level = switch (Objects.toString(req.getParameter("level"), "")) {
                    case "basic" -> "cơ bản";
                    case "apply" -> "vận dụng";
                    case "advanced" -> "nâng cao";
                    default -> throw new IllegalArgumentException("Mức độ không hợp lệ.");
                };
                String count = Objects.toString(req.getParameter("count"), "");
                if (!Set.of("1", "2", "3").contains(count))
                    throw new IllegalArgumentException("Chọn từ 1 đến 3 câu.");
                String prompt = "Soạn đúng " + count + " câu tự luận Vật lý 10 về "
                        + topic + ", mức " + level + ". Chỉ viết đề, đủ dữ kiện."
                        + " Dùng văn bản thuần, không Markdown."
                        + " Mỗi câu bắt đầu trên dòng mới, dạng Câu 1: nội dung.";
                String practice = AIService.getInstance().tutor(u,
                        List.of(Map.of("role", "user", "content", prompt)));
                var labels = java.util.regex.Pattern
                        .compile("(?m)^\\h*Câu\\h+([1-9]\\d*):\\h*\\S")
                        .matcher(practice);
                int next = 1;
                while (labels.find()) {
                    if (!Integer.toString(next++).equals(labels.group(1)))
                        throw new IllegalArgumentException("Thứ tự câu AI chưa đúng.");
                }
                if (next - 1 != Integer.parseInt(count))
                    throw new IllegalArgumentException("Đề AI chưa đủ số câu yêu cầu.");
                req.getSession().setAttribute("practiceEssay", practice);
                resp.sendRedirect(dest);
                return true;
            }
                 */

                String course = req.getParameter("course");
                if (!com.smartstudy.service.CourseService.getInstance().owns(u, course)) {
                    throw new IllegalArgumentException("Chọn khóa học do bạn phụ trách.");
                }
                var a
                        = s.save(
                                u,
                                req.getParameter("id"),
                                req.getParameter("title"),
                                req.getParameter("prompt"),
                                req.getParameter("instructions"),
                                req.getParameter("reference"),
                                req.getParameter("rubric"),
                                req.getParameter("deadline"),
                                "published".equals(req.getParameter("status")),
                                Integer.parseInt(req.getParameter("version")));
                com.smartstudy.service.CourseService.getInstance()
                        .assignAssessment(u, a.id(), course);
                dest += "?id=" + a.id();
            } else if ("draft".equals(action)) {
                s.draft(u, req.getParameter("id"), req.getParameter("answer"));
                dest += "?id=" + req.getParameter("id");
            } else if ("submit".equals(action)) {
                String token = req.getParameter("token");
                if (token == null || token.length() != 36) {
                    throw new IllegalArgumentException("Phiên nộp bài không hợp lệ.");
                }
                var sub = s.submit(u, req.getParameter("id"), req.getParameter("answer"), token);
                dest += "?submission=" + sub.id();
                req.getSession().setAttribute("essayAutoFeedback", sub.id());
                req.getSession().setAttribute("essayToken", UUID.randomUUID().toString());
            } else if ("student-feedback".equals(action)) {
                String submission = req.getParameter("submission");
                s.studentFeedback(u, submission);
                dest += "?submission=" + submission;
            } else if ("review".equals(action)) {
                var sub = s.submission(req.getParameter("submission"), u);
                if (sub == null) {
                    throw new IllegalArgumentException("Bài nộp không tồn tại.");
                }
                List<Double> scores = new ArrayList<>();
                for (int i = 0; i < sub.assignment().rubric().size(); i++) {
                    scores.add(Double.parseDouble(req.getParameter("score_" + i)));
                }
                s.review(
                        u,
                        sub.id(),
                        scores,
                        req.getParameter("feedback"),
                        "published".equals(req.getParameter("status")),
                        Integer.parseInt(req.getParameter("version")));
                dest += "?submission=" + sub.id();
            } else if ("ai-review".equals(action)) {
                s.suggest(u, req.getParameter("submission"));
                dest += "?submission=" + req.getParameter("submission");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            req.getSession()
                    .setAttribute(
                            "flash",
                            "Đã lưu. Kết quả chỉ hiển thị cho học sinh sau khi giáo viên công bố.");
            resp.sendRedirect(dest);
        } catch (IllegalArgumentException e) {
            req.setAttribute(
                    "essayError",
                    e instanceof NumberFormatException
                            ? "Điểm hoặc phiên bản không hợp lệ."
                            : e.getMessage());
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
