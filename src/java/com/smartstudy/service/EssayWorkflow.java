package com.smartstudy.service;

import author.*;
import java.time.*;
import java.util.*;

/** Shared assignments and durable reviewed results. No automatic AI publication. */
public final class EssayWorkflow {
    public record Criterion(String title, double max) {}

    public record Assignment(
            String id,
            String owner,
            String title,
            String prompt,
            String instructions,
            String reference,
            List<Criterion> rubric,
            LocalDate deadline,
            boolean published,
            int version) {
        public Assignment {
            rubric = List.copyOf(rubric);
        }

        public double maximum() {
            return rubric.stream().mapToDouble(Criterion::max).sum();
        }
    }

    public record Review(
            Instant at, String reviewer, List<Double> scores, String feedback, boolean published) {
        public Review {
            scores = List.copyOf(scores);
        }

        public double total() {
            return scores.stream().mapToDouble(Double::doubleValue).sum();
        }
    }

    public record Submission(
            String id,
            Assignment assignment,
            String email,
            String answer,
            Instant at,
            List<Review> reviews) {
        public Submission {
            reviews = List.copyOf(reviews);
        }

        public Review latest() {
            return reviews.isEmpty() ? null : reviews.get(reviews.size() - 1);
        }

        public Review visible() {
            return reviews.stream().filter(Review::published).reduce((a, b) -> b).orElse(null);
        }
    }

    private static final EssayWorkflow INSTANCE = new EssayWorkflow();

    public static EssayWorkflow getInstance() {
        return INSTANCE;
    }

    private final Map<String, Assignment> assignments =
            com.smartstudy.persistence.Stores.map("essay_assignments", Assignment.class);
    private final Map<String, Submission> submissions =
            com.smartstudy.persistence.Stores.map("essay_submissions", Submission.class);
    private final Map<String, String> drafts =
            com.smartstudy.persistence.Stores.map("essay_drafts", String.class);
    private final Map<String, com.smartstudy.model.EssayGradingResult> suggestions =
            com.smartstudy.persistence.Stores.map(
                    "essay_ai_suggestions", com.smartstudy.model.EssayGradingResult.class);

    public synchronized com.smartstudy.model.EssayGradingResult suggestion(String id, User u) {
        return u.getRole() == Role.TEACHER && submission(id, u) != null
                ? suggestions.get(id)
                : null;
    }

    public void suggest(User u, String id) {
        Submission s = submission(id, u);
        if (u.getRole() != Role.TEACHER || s == null)
            throw new IllegalArgumentException("Bài nộp không thuộc khóa học của bạn.");
        var photos = new EssayPhotoService();
        var photo = photos.submitted(u, id);
        String answer = s.answer();
        byte[] image = null;
        if (photo != null) {
            photo = photos.scan(u, photo.id());
            if (photo.transcription() == null
                    || photo.transcription().isBlank()
                    || photo.confidence() == null
                    || photo.confidence() < .5
                    || photo.transcription().contains("[không đọc rõ]"))
                throw new IllegalArgumentException(
                        "Ảnh chưa đọc đủ rõ để đề xuất điểm. Hãy xem ảnh và chấm thủ công; không tự"
                            + " suy đoán phần bài làm bị mờ.");
            answer += "\n\nNội dung đọc từ ảnh bài làm:\n" + photo.transcription();
            image = photos.bytes(u, photo.id());
        }
        var result = AIService.getInstance().grade(u, s, answer, image);
        if (photo != null) {
            result =
                    new com.smartstudy.model.EssayGradingResult(
                            result.scores(),
                            result.comments(),
                            result.evidence(),
                            result.strengths(),
                            result.errors(),
                            result.improvements()
                                    + "\nĐối chiếu ảnh gốc trước khi công bố. "
                                    + photo.warning(),
                            Math.min(result.confidence(), photo.confidence()),
                            result.at());
        }
        synchronized (this) {
            suggestions.put(id, result);
        }
        AuditService.record(u.getEmail(), "AI đề xuất chấm", "Chờ giáo viên duyệt");
    }

    private EssayWorkflow() {
        if (!com.smartstudy.persistence.Stores.seed() || !assignments.isEmpty()) return;
        assignments.put(
                "newton",
                new Assignment(
                        "newton",
                        "teacher@smartstudy.vn",
                        "Vận dụng định luật II Newton",
                        "Một vật có khối lượng 2 kg chịu hợp lực 10 N. Tính gia tốc, trình bày các"
                            + " bước giải và giải thích hướng của gia tốc.",
                        "Nêu dữ kiện, chọn công thức, thay số với đơn vị SI và kết luận bằng lời.",
                        "Theo định luật II Newton: a = F/m = 10/2 = 5 m/s². Gia tốc cùng hướng với"
                            + " hợp lực tác dụng lên vật.",
                        List.of(
                                new Criterion("Nêu đúng định luật và công thức", 3),
                                new Criterion("Thay số và tính toán đúng", 3),
                                new Criterion("Đơn vị, hướng và lập luận", 4)),
                        null,
                        true,
                        1));
    }

    private static String text(String s, int max) {
        if (s == null || s.isBlank() || s.length() > max)
            throw new IllegalArgumentException("Nội dung trống hoặc vượt quá độ dài cho phép.");
        return s.trim();
    }

    public synchronized List<Assignment> assignments(User u) {
        return assignments.values().stream()
                .filter(
                        a ->
                                u.getRole() == Role.STUDENT
                                        ? a.published()
                                                && CourseService.getInstance().canLearn(u, a.id())
                                        : a.owner().equals(u.getEmail()))
                .toList();
    }

    public synchronized Assignment assignment(String id, User u) {
        return assignments(u).stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
    }

    public synchronized Assignment save(
            User u,
            String id,
            String title,
            String prompt,
            String instructions,
            String reference,
            String rubric,
            String deadline,
            boolean published,
            int version) {
        if (u.getRole() != Role.TEACHER)
            throw new IllegalArgumentException("Chỉ giáo viên có thể giao bài.");
        Assignment old = id == null || id.isBlank() ? null : assignment(id, u);
        if (id != null && !id.isBlank() && old == null)
            throw new IllegalArgumentException("Không tìm thấy bài của bạn.");
        if (old != null && old.version() != version)
            throw new IllegalArgumentException(
                    "Bài đã thay đổi ở tab khác. Tải lại trước khi sửa.");
        List<Criterion> criteria = new ArrayList<>();
        for (String line : text(rubric, 2000).split("\\R")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 2)
                throw new IllegalArgumentException(
                        "Mỗi tiêu chí cần dạng Tên tiêu chí | điểm tối đa.");
            double max;
            try {
                max = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Điểm rubric phải là số.");
            }
            if (!Double.isFinite(max) || max <= 0 || max > 100)
                throw new IllegalArgumentException(
                        "Điểm tiêu chí phải lớn hơn 0 và không quá 100.");
            criteria.add(new Criterion(text(parts[0], 200), max));
        }
        if (criteria.isEmpty()
                || criteria.size() > 10
                || criteria.stream().mapToDouble(Criterion::max).sum() > 100)
            throw new IllegalArgumentException(
                    "Rubric cần 1–10 tiêu chí, tổng điểm không quá 100.");
        LocalDate due = null;
        if (deadline != null && !deadline.isBlank())
            try {
                due = LocalDate.parse(deadline);
            } catch (Exception e) {
                throw new IllegalArgumentException("Ngày hết hạn không hợp lệ.");
            }
        Assignment a =
                new Assignment(
                        old == null ? UUID.randomUUID().toString() : old.id(),
                        u.getEmail(),
                        text(title, 150),
                        text(prompt, 6000),
                        text(instructions, 3000),
                        text(reference, 6000),
                        criteria,
                        due,
                        published,
                        old == null ? 1 : old.version() + 1);
        assignments.put(a.id(), a);
        return a;
    }

    public synchronized String draft(User u, String id) {
        return drafts.getOrDefault(u.getEmail() + ":" + id, "");
    }

    public synchronized void draft(User u, String id, String answer) {
        if (u.getRole() != Role.STUDENT || assignment(id, u) == null)
            throw new IllegalArgumentException("Bài không khả dụng.");
        if (answer == null || answer.length() > 12000)
            throw new IllegalArgumentException("Bản nháp vượt quá 12.000 ký tự.");
        drafts.put(u.getEmail() + ":" + id, answer);
    }

    public synchronized Submission submit(User u, String id, String answer, String token) {
        if (u.getRole() != Role.STUDENT)
            throw new IllegalArgumentException("Chỉ học sinh được nộp bài.");
        String key = u.getEmail() + ":" + token;
        if (submissions.containsKey(key)) return submissions.get(key);
        Assignment a = assignment(id, u);
        if (a == null) throw new IllegalArgumentException("Bài không còn mở.");
        if (a.deadline() != null && LocalDate.now().isAfter(a.deadline()))
            throw new IllegalArgumentException("Đã quá thời hạn nộp bài.");
        var photos = new EssayPhotoService();
        var photo = photos.draft(u, id);
        if ((answer == null || answer.isBlank()) && photo != null)
            answer = "Bài làm được nộp bằng ảnh đính kèm.";
        answer = text(answer, 12000);
        if (answer.length() < 20 && photo == null)
            throw new IllegalArgumentException(
                    "Bài làm cần ít nhất 20 ký tự hoặc một ảnh bài làm.");
        Submission s =
                new Submission(
                        UUID.randomUUID().toString(),
                        a,
                        u.getEmail(),
                        answer,
                        Instant.now(),
                        List.of());
        submissions.put(key, s);
        photos.attach(u, id, s.id());
        drafts.remove(u.getEmail() + ":" + id);
        LearningService.getInstance().log(u.getEmail(), "Nộp tự luận", a.title());
        return s;
    }

    public synchronized List<Submission> submissions(User u) {
        return submissions.values().stream()
                .filter(
                        s ->
                                u.getRole() == Role.STUDENT
                                        ? s.email().equals(u.getEmail())
                                        : s.assignment().owner().equals(u.getEmail()))
                .sorted(Comparator.comparing(Submission::at).reversed())
                .toList();
    }

    public synchronized Submission submission(String id, User u) {
        return submissions(u).stream().filter(s -> s.id().equals(id)).findFirst().orElse(null);
    }

    public synchronized void review(
            User u, String id, List<Double> scores, String feedback, boolean publish, int version) {
        if (u.getRole() != Role.TEACHER)
            throw new IllegalArgumentException("Chỉ giáo viên được chấm.");
        Submission s = submission(id, u);
        if (s == null) throw new IllegalArgumentException("Không tìm thấy bài nộp.");
        if (version != s.reviews().size())
            throw new IllegalArgumentException(
                    "Kết quả đã thay đổi ở tab khác. Hãy tải lại trang.");
        if (scores.size() != s.assignment().rubric().size())
            throw new IllegalArgumentException("Cần chấm đủ tiêu chí.");
        for (int i = 0; i < scores.size(); i++)
            if (!Double.isFinite(scores.get(i))
                    || scores.get(i) < 0
                    || scores.get(i) > s.assignment().rubric().get(i).max())
                throw new IllegalArgumentException("Điểm vượt thang điểm của tiêu chí.");
        List<Review> reviews = new ArrayList<>(s.reviews());
        reviews.add(new Review(Instant.now(), u.getEmail(), scores, text(feedback, 6000), publish));
        Submission updated =
                new Submission(s.id(), s.assignment(), s.email(), s.answer(), s.at(), reviews);
        submissions.replaceAll((k, v) -> v.id().equals(id) ? updated : v);
        if (publish)
            LearningService.getInstance()
                    .log(s.email(), "Nhận kết quả tự luận", s.assignment().title());
    }

    public synchronized int count() {
        return submissions.size();
    }

    public synchronized List<Submission> monitoring(User u) {
        return u.getRole() == Role.ADMIN ? List.copyOf(submissions.values()) : submissions(u);
    }

    public synchronized int assignmentCount(User u) {
        return u.getRole() == Role.ADMIN ? assignments.size() : assignments(u).size();
    }
}
