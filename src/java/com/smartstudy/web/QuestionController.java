package com.smartstudy.web;

import author.*;
import com.smartstudy.model.*;
import com.smartstudy.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public final class QuestionController {
    private QuestionController() {}

    private static boolean handles(HttpServletRequest r) {
        return Set.of("ai-questions", "question-bank").contains(ViewSupport.current(r));
    }

    public static boolean get(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!handles(req)) return false;
        if (((User) req.getSession().getAttribute("user")).getRole() != Role.TEACHER) {
            resp.sendError(403);
            return true;
        }
        var bank = QuestionBankService.getInstance();
        var questions = bank.owned((User) req.getSession().getAttribute("user"));
        req.setAttribute("questionBank", questions);
        Map<String, Integer> versions = new HashMap<>();
        Set<String> archived = new HashSet<>();
        for (Question q : questions) {
            versions.put(q.id(), bank.version(q.id()));
            if (bank.archived(q.id())) archived.add(q.id());
        }
        req.setAttribute("bankVersions", versions);
        req.setAttribute("archivedQuestions", archived);
        req.setAttribute("aiAvailable", AIService.getInstance().available());
        req.getRequestDispatcher("/WEB-INF/views/teacher/" + ViewSupport.current(req) + ".jsp")
                .forward(req, resp);
        return true;
    }

    private static Question parse(HttpServletRequest req, Question q) {
        String id = q.id();
        List<String> options = new ArrayList<>();
        for (int i = 0; i < q.options().size(); i++)
            options.add(
                    CatalogService.required(
                            req.getParameter("option_" + id + "_" + i), 300, "Phương án"));
        if (new HashSet<>(options).size() != options.size())
            throw new IllegalArgumentException("Các phương án cần khác nhau.");
        String level = req.getParameter("level_" + id);
        if (level == null
                || !Set.of("Nhận biết", "Thông hiểu", "Vận dụng", "Vận dụng cao").contains(level))
            throw new IllegalArgumentException("Mức độ không hợp lệ.");
        int points = Integer.parseInt(req.getParameter("points_" + id));
        if (points < 1 || points > 10) throw new IllegalArgumentException("Điểm từ 1 đến 10.");
        return new Question(
                id,
                CatalogService.required(req.getParameter("question_" + id), 1000, "Câu hỏi"),
                options,
                Integer.parseInt(req.getParameter("correct_" + id)),
                CatalogService.required(req.getParameter("explanation_" + id), 2000, "Lời giải"),
                q.topic(),
                level,
                points,
                q.source(),
                q.lesson(),
                q.objective(),
                req.getParameter("hardness_" + id) == null
                        ? q.hardness()
                        : req.getParameter("hardness_" + id),
                req.getParameter("seconds_" + id) == null
                        ? q.seconds()
                        : Integer.parseInt(req.getParameter("seconds_" + id)));
    }

    @SuppressWarnings("unchecked")
    public static boolean post(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        if (!handles(req)) return false;
        if (!ViewSupport.validCsrf(req)
                || ((User) req.getSession().getAttribute("user")).getRole() != Role.TEACHER) {
            resp.sendError(403);
            return true;
        }
        var bank = QuestionBankService.getInstance();
        User actor = (User) req.getSession().getAttribute("user");
        String dest = req.getContextPath() + "/teacher/" + ViewSupport.current(req);
        try {
            String action = req.getParameter("action");
            List<Question> pool =
                    (List<Question>) req.getSession().getAttribute("generatedQuestions");
            if ("generate".equals(action)) {
                var input =
                        new AIQuestionRequest(
                                req.getParameter("topic"),
                                Integer.parseInt(req.getParameter("count")),
                                req.getParameter("level"),
                                Integer.parseInt(req.getParameter("points")),
                                req.getParameter("lesson") == null
                                        ? "Vật lý 10"
                                        : req.getParameter("lesson"),
                                req.getParameter("objective") == null
                                        ? "Vận dụng kiến thức đúng và ghi rõ đơn vị SI"
                                        : req.getParameter("objective"),
                                req.getParameter("hardness"),
                                Integer.parseInt(req.getParameter("seconds")),
                                req.getParameter("extra") == null ? "" : req.getParameter("extra"));
                int offset =
                        (int)
                                Optional.ofNullable(
                                                req.getSession().getAttribute("generationOffset"))
                                        .orElse(0);
                boolean live = "live".equals(req.getParameter("mode"));
                var result =
                        live
                                ? AIService.getInstance().generate(actor, input)
                                : new AIQuestionGeneratorService().generate(input, offset);
                req.getSession().setAttribute("generationOffset", offset + 1);
                req.getSession().setAttribute("generatedQuestions", result);
                req.getSession().setAttribute("lastAIQuestionRequest", input);
                req.getSession().setAttribute("generatedMode", live ? "live" : "demo");
                req.getSession()
                        .setAttribute(
                                "flash",
                                "Đã chuẩn bị "
                                        + result.size()
                                        + " câu. Hãy rà soát kiến thức, đáp án và đơn vị trước khi"
                                        + " lưu.");
            } else if (action != null
                    && (action.startsWith("regenerate_") || action.startsWith("remove_"))) {
                if (pool == null) throw new IllegalArgumentException("Chưa có bộ xem trước.");
                boolean regenerate = action.startsWith("regenerate_");
                String id = action.substring(regenerate ? 11 : 7);
                Question original =
                        pool.stream()
                                .filter(q -> q.id().equals(id))
                                .findFirst()
                                .orElseThrow(
                                        () -> new IllegalArgumentException("Câu không tồn tại."));
                List<Question> updated = new ArrayList<>();
                for (Question q : pool) if (!q.id().equals(id)) updated.add(parse(req, q));
                if (regenerate) {
                    var input =
                            new AIQuestionRequest(
                                    original.topic(),
                                    1,
                                    original.difficulty(),
                                    original.points(),
                                    original.lesson(),
                                    original.objective(),
                                    original.hardness(),
                                    original.seconds(),
                                    "");
                    int offset =
                            (int)
                                            Optional.ofNullable(
                                                            req.getSession()
                                                                    .getAttribute(
                                                                            "generationOffset"))
                                                    .orElse(0)
                                    + pool.size()
                                    + pool.indexOf(original);
                    var generated =
                            "live".equals(req.getSession().getAttribute("generatedMode"))
                                    ? AIService.getInstance().generate(actor, input)
                                    : new AIQuestionGeneratorService().generate(input, offset);
                    if (generated.stream()
                            .anyMatch(
                                    g ->
                                            updated.stream()
                                                    .anyMatch(
                                                            old ->
                                                                    old.text()
                                                                            .equalsIgnoreCase(
                                                                                    g.text()))))
                        throw new IllegalArgumentException(
                                "Câu tạo lại trùng bộ hiện tại. Hãy tạo bộ mới hoặc chỉnh sửa thủ"
                                    + " công.");
                    updated.addAll(Math.min(pool.indexOf(original), updated.size()), generated);
                    req.getSession().setAttribute("generationOffset", offset + 1);
                }
                req.getSession().setAttribute("generatedQuestions", List.copyOf(updated));
                req.getSession()
                        .setAttribute(
                                "flash",
                                regenerate
                                        ? "Đã tạo lại câu; các chỉnh sửa ở câu khác được giữ."
                                        : "Đã bỏ câu khỏi bộ xem trước.");
            } else if ("manual-create".equals(action)) {
                Question q =
                        parse(
                                req,
                                new Question(
                                        "manual",
                                        "new",
                                        List.of("A", "B", "C", "D"),
                                        0,
                                        "",
                                        "Giáo viên tự chọn",
                                        "Vận dụng",
                                        1,
                                        "Giáo viên biên soạn"));
                Question saved =
                        new Question(
                                null,
                                q.text(),
                                q.options(),
                                q.correctIndex(),
                                q.explanation(),
                                CatalogService.required(req.getParameter("topic"), 80, "Chủ đề"),
                                q.difficulty(),
                                q.points(),
                                q.source());
                int count = bank.addSelected(actor, List.of(saved));
                req.getSession()
                        .setAttribute(
                                "flash",
                                count == 1
                                        ? "Đã thêm câu giáo viên biên soạn."
                                        : "Nội dung trùng câu đã có; không thêm lại.");
            } else if ("save-selected".equals(action)) {
                if (pool == null) throw new IllegalArgumentException("Tạo câu hỏi trước khi lưu.");
                String[] ids = req.getParameterValues("selected");
                if (ids == null || ids.length == 0)
                    throw new IllegalArgumentException("Chọn ít nhất một câu để lưu.");
                List<Question> selected = new ArrayList<>();
                for (String id : new LinkedHashSet<>(Arrays.asList(ids))) {
                    Question q =
                            pool.stream()
                                    .filter(x -> x.id().equals(id))
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "Câu chọn không thuộc bộ xem trước."));
                    selected.add(parse(req, q));
                }
                int added = bank.addSelected(actor, selected);
                req.getSession()
                        .setAttribute(
                                "flash",
                                "Đã thêm "
                                        + added
                                        + " câu. Bỏ qua "
                                        + (selected.size() - added)
                                        + " câu trùng nội dung.");
                dest = req.getContextPath() + "/teacher/question-bank";
            } else if ("update-question".equals(action)) {
                Question q =
                        bank.owned(actor).stream()
                                .filter(x -> x.id().equals(req.getParameter("id")))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Không tìm thấy câu hỏi."));
                bank.update(actor, parse(req, q), Integer.parseInt(req.getParameter("version")));
                req.getSession()
                        .setAttribute(
                                "flash",
                                "Đã sửa câu hỏi. Quiz đã lưu giữ nguyên bản câu hỏi tại thời điểm"
                                    + " biên soạn.");
            } else if ("archive-question".equals(action) || "restore-question".equals(action)) {
                if (bank.owned(actor).stream()
                        .noneMatch(q -> q.id().equals(req.getParameter("id"))))
                    throw new IllegalArgumentException("Không có quyền với câu hỏi này.");
                bank.archive(req.getParameter("id"), "archive-question".equals(action));
                req.getSession().setAttribute("flash", "Đã cập nhật trạng thái câu hỏi.");
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            resp.sendRedirect(dest);
        } catch (IllegalArgumentException e) {
            req.setAttribute(
                    "questionError",
                    e instanceof NumberFormatException
                            ? "Điểm hoặc đáp án không hợp lệ."
                            : e.getMessage());
            resp.setStatus(400);
            get(req, resp);
        }
        return true;
    }
}
