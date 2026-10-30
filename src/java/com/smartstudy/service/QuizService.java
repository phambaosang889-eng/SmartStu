package com.smartstudy.service;

import author.*;
import com.smartstudy.model.*;
import java.time.*;
import java.util.*;

public final class QuizService {
    public record Run(
            String token, String email, QuizDefinition quiz, Instant started, Instant deadline) {}

    public record Result(
            Run run,
            List<Integer> answers,
            int score,
            int correct,
            boolean expired,
            Instant finished) {
        public Result {
            answers = List.copyOf(answers);
        }
    }

    private static final QuizService INSTANCE = new QuizService();
    private final Map<String, QuizDefinition> quizzes =
            com.smartstudy.persistence.Stores.map("quizzes", QuizDefinition.class);
    private final Map<String, Run> runs =
            com.smartstudy.persistence.Stores.map("quiz_runs", Run.class);
    private final Map<String, Result> results =
            com.smartstudy.persistence.Stores.map("quiz_attempts", Result.class);

    private QuizService() {
        if (!com.smartstudy.persistence.Stores.seed() || !quizzes.isEmpty()) return;
        quizzes.put(
                "newton-demo",
                new QuizDefinition(
                        "newton-demo",
                        "Luyện tập định luật Newton",
                        "Vận dụng F = ma, xác định lực tổng hợp và gia tốc. Mỗi câu chọn một đáp án"
                            + " đúng.",
                        8,
                        "published",
                        "teacher@smartstudy.vn",
                        QuestionBankService.getInstance().generateDemo("Định luật Newton", 3),
                        1));
    }

    public static QuizService getInstance() {
        return INSTANCE;
    }

    public synchronized List<QuizDefinition> list(User u) {
        return quizzes.values().stream()
                .filter(
                        q ->
                                u.getRole() == Role.STUDENT
                                        ? q.status().equals("published")
                                                && CourseService.getInstance().canLearn(u, q.id())
                                        : u.getRole() == Role.ADMIN
                                                || q.owner().equals(u.getEmail()))
                .toList();
    }

    public synchronized QuizDefinition get(String id, User u) {
        return list(u).stream().filter(q -> q.id().equals(id)).findFirst().orElse(null);
    }

    public synchronized List<Question> questions() {
        Map<String, Question> all = new LinkedHashMap<>();
        quizzes.values().forEach(q -> q.questions().forEach(x -> all.put(x.id(), x)));
        QuestionBankService.getInstance().active().forEach(q -> all.put(q.id(), q));
        return List.copyOf(all.values());
    }

    public synchronized List<Question> questions(User u) {
        if (u.getRole() == Role.STUDENT) return List.of();
        Map<String, Question> all = new LinkedHashMap<>();
        list(u).forEach(q -> q.questions().forEach(x -> all.put(x.id(), x)));
        QuestionBankService.getInstance().active(u).forEach(q -> all.put(q.id(), q));
        return List.copyOf(all.values());
    }

    public synchronized QuizDefinition save(
            User u,
            String id,
            String title,
            String description,
            int minutes,
            String status,
            List<String> selected,
            int version) {
        String course =
                id == null || id.isBlank()
                        ? CourseService.getInstance().courses(u).stream()
                                .findFirst()
                                .map(CatalogEntry::id)
                                .orElse("")
                        : CourseService.getInstance().courseOf(id);
        return save(
                u,
                id,
                title,
                description,
                minutes,
                status,
                selected,
                version,
                null,
                0,
                100,
                course);
    }

    public synchronized QuizDefinition save(
            User u,
            String id,
            String title,
            String description,
            int minutes,
            String status,
            List<String> selected,
            int version,
            LocalDateTime due,
            int maxAttempts,
            int maxScore,
            String course) {
        if (u.getRole() != Role.TEACHER)
            throw new IllegalArgumentException("Chỉ giáo viên phụ trách được soạn quiz.");
        if (!CourseService.getInstance().owns(u, course))
            throw new IllegalArgumentException("Chọn khóa học của bạn.");
        if (maxAttempts < 0 || maxAttempts > 100 || maxScore < 1 || maxScore > 1000)
            throw new IllegalArgumentException(
                    "Số lần làm từ 0–100 (0: không giới hạn), tổng điểm từ 1–1000.");
        title = CatalogService.required(title, 150, "Tên quiz");
        description = CatalogService.required(description, 1500, "Mô tả");
        if (minutes < 1
                || minutes > 180
                || status == null
                || !Set.of("draft", "published", "archived").contains(status))
            throw new IllegalArgumentException(
                    "Thời gian phải từ 1–180 phút và trạng thái phải hợp lệ.");
        Map<String, Question> available = new HashMap<>();
        questions(u).forEach(q -> available.put(q.id(), q));
        List<Question> qs = new ArrayList<>();
        for (String key : new LinkedHashSet<>(selected)) {
            Question question = available.get(key);
            if (question == null)
                throw new IllegalArgumentException(
                        "Câu hỏi không thuộc ngân hàng hoặc quiz của bạn.");
            qs.add(question);
        }
        if (qs.isEmpty() || qs.size() > 50)
            throw new IllegalArgumentException("Chọn từ 1 đến 50 câu hỏi.");
        QuizDefinition old = id == null || id.isBlank() ? null : get(id, u);
        if (id != null && !id.isBlank() && old == null)
            throw new IllegalArgumentException("Không tìm thấy quiz hoặc bạn không có quyền sửa.");
        if (old != null && old.version() != version)
            throw new IllegalArgumentException("Quiz đã được cập nhật ở tab khác. Tải lại trang.");
        QuizDefinition q =
                new QuizDefinition(
                        old == null ? UUID.randomUUID().toString() : old.id(),
                        title,
                        description,
                        minutes,
                        status,
                        u.getEmail(),
                        qs,
                        old == null ? 1 : old.version() + 1,
                        due,
                        maxAttempts,
                        maxScore);
        quizzes.put(q.id(), q);
        CourseService.getInstance().assignAssessment(u, q.id(), course);
        return q;
    }

    public synchronized QuizDefinition copy(String id, User u) {
        QuizDefinition q = get(id, u);
        if (q == null || u.getRole() != Role.TEACHER)
            throw new IllegalArgumentException("Không có quyền sao chép quiz.");
        return save(
                u,
                null,
                q.title() + " · bản sao",
                q.description(),
                q.minutes(),
                "draft",
                q.questions().stream().map(Question::id).toList(),
                0,
                q.due(),
                q.maxAttempts(),
                q.maxScore(),
                CourseService.getInstance().courseOf(q.id()));
    }

    public synchronized Run start(String id, User u) {
        QuizDefinition q = get(id, u);
        if (q == null || !q.status().equals("published") || u.getRole() != Role.STUDENT)
            throw new IllegalArgumentException("Quiz chưa được phát hành.");
        for (Run r : runs.values())
            if (r.email().equals(u.getEmail())
                    && r.quiz().id().equals(id)
                    && !results.containsKey(r.token())
                    && r.deadline().isAfter(Instant.now())) return r;
        if (q.due() != null
                && !q.due().atZone(ZoneId.of("Asia/Bangkok")).toInstant().isAfter(Instant.now()))
            throw new IllegalArgumentException("Quiz đã hết hạn.");
        if (q.maxAttempts() > 0
                && history(u).stream().filter(r -> r.run().quiz().id().equals(id)).count()
                        >= q.maxAttempts())
            throw new IllegalArgumentException("Bạn đã dùng hết số lần làm bài.");
        if (com.smartstudy.persistence.Database.memory() && runs.size() > 2000)
            throw new IllegalArgumentException(
                    "Số phiên kiểm thử trong bộ nhớ đã đạt giới hạn. Liên hệ quản trị viên.");
        Instant now = Instant.now(), deadline = now.plusSeconds(q.minutes() * 60L);
        if (q.due() != null
                && q.due().atZone(ZoneId.of("Asia/Bangkok")).toInstant().isBefore(deadline))
            deadline = q.due().atZone(ZoneId.of("Asia/Bangkok")).toInstant();
        Run r = new Run(UUID.randomUUID().toString(), u.getEmail(), q, now, deadline);
        runs.put(r.token(), r);
        return r;
    }

    public synchronized Run run(String token, User u) {
        Run r = runs.get(token);
        return r != null && r.email().equals(u.getEmail()) ? r : null;
    }

    public synchronized Result result(String token, User u) {
        Result r = results.get(token);
        return r != null && r.run().email().equals(u.getEmail()) ? r : null;
    }

    public synchronized Result submit(String token, User u, Map<String, String[]> params) {
        Run r = run(token, u);
        if (r == null) throw new IllegalArgumentException("Phiên làm bài không hợp lệ.");
        if (results.containsKey(token)) return results.get(token);
        boolean expired = Instant.now().isAfter(r.deadline().plusSeconds(15));
        List<Integer> answers = new ArrayList<>();
        int correct = 0, points = 0, total = 0;
        for (Question q : r.quiz().questions()) {
            String[] raw = params.get("answer_" + q.id());
            int a = -1;
            if (raw != null) {
                if (raw.length != 1)
                    throw new IllegalArgumentException("Mỗi câu chỉ được chọn một đáp án.");
                try {
                    a = Integer.parseInt(raw[0]);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Đáp án không hợp lệ.");
                }
                if (a < 0 || a >= q.options().size())
                    throw new IllegalArgumentException("Đáp án ngoài phạm vi.");
            }
            answers.add(a);
            total += q.points();
            if (!expired && a == q.correctIndex()) {
                correct++;
                points += q.points();
            }
        }
        Result result =
                new Result(
                        r,
                        answers,
                        total == 0
                                ? 0
                                : (int) Math.round(points * (double) r.quiz().maxScore() / total),
                        correct,
                        expired,
                        Instant.now());
        results.put(token, result);
        LearningService.getInstance()
                .quiz(u.getEmail(), (int) Math.round(result.score() * 100.0 / r.quiz().maxScore()));
        return result;
    }

    public synchronized List<Result> history(User u) {
        return results.values().stream()
                .filter(
                        r ->
                                u.getRole() == Role.STUDENT
                                        ? r.run().email().equals(u.getEmail())
                                        : u.getRole() == Role.ADMIN
                                                || r.run().quiz().owner().equals(u.getEmail()))
                .toList();
    }
}
