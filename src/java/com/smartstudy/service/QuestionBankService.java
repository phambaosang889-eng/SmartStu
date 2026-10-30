package com.smartstudy.service;

import com.smartstudy.model.Question;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Shared question bank with durable ownership, revisions and archive state. */
public final class QuestionBankService {
    private static final QuestionBankService INSTANCE = new QuestionBankService();
    private final java.util.Map<String, Question> questions =
            com.smartstudy.persistence.Stores.map("question_bank", Question.class);
    private final java.util.Map<String, Boolean> archived =
            com.smartstudy.persistence.Stores.map("question_archived", Boolean.class);
    private final java.util.Map<String, String> owners =
            com.smartstudy.persistence.Stores.map("question_owners", String.class);
    private final java.util.Map<String, Integer> versions =
            com.smartstudy.persistence.Stores.map("question_versions", Integer.class);

    public synchronized int version(String id) {
        return versions.getOrDefault(id, 1);
    }

    public synchronized void update(author.User u, Question q, int version) {
        if (owned(u).stream().noneMatch(x -> x.id().equals(q.id())))
            throw new IllegalArgumentException("Không có quyền sửa câu hỏi.");
        if (version(q.id()) != version)
            throw new IllegalArgumentException("Câu hỏi đã được sửa ở tab khác. Tải lại trang.");
        if (owned(u).stream()
                .anyMatch(x -> !x.id().equals(q.id()) && x.text().equalsIgnoreCase(q.text())))
            throw new IllegalArgumentException("Nội dung trùng câu hỏi đã có.");
        update(q);
        versions.put(q.id(), version + 1);
    }

    private QuestionBankService() {}

    public static QuestionBankService getInstance() {
        return INSTANCE;
    }

    public List<Question> generateDemo(String topic, int count) {
        if (count < 1 || count > 10) throw new IllegalArgumentException("Số câu phải từ 1 đến 10.");
        String safeTopic = "Định luật Newton";
        List<Question> generated = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (i == 9) {
                generated.add(
                        new Question(
                                null,
                                "Khi hợp lực tác dụng lên vật bằng 0, vật đang chuyển động sẽ thế"
                                    + " nào?",
                                List.of(
                                        "Dừng lại ngay",
                                        "Tiếp tục thẳng đều",
                                        "Tăng tốc",
                                        "Đổi hướng liên tục"),
                                1,
                                "Theo định luật I Newton, hợp lực bằng 0 thì vật giữ nguyên trạng"
                                    + " thái chuyển động.",
                                safeTopic,
                                "Thông hiểu",
                                1,
                                "Dữ liệu mẫu"));
            } else {
                int mass = i + 2, acceleration = i + 3, force = mass * acceleration;
                generated.add(
                        new Question(
                                null,
                                "Vật m = "
                                        + mass
                                        + " kg chịu lực tổng hợp F = "
                                        + force
                                        + " N. Gia tốc bằng bao nhiêu?",
                                List.of(
                                        (acceleration - 1) + " m/s²",
                                        acceleration + " m/s²",
                                        (acceleration + 1) + " m/s²",
                                        (acceleration + 2) + " m/s²"),
                                1,
                                "Áp dụng a = F/m = "
                                        + force
                                        + "/"
                                        + mass
                                        + " = "
                                        + acceleration
                                        + " m/s².",
                                safeTopic,
                                "Vận dụng",
                                1,
                                "Dữ liệu mẫu"));
            }
        }
        return generated;
    }

    public synchronized int addSelected(List<Question> selectedQuestions) {
        int added = 0;
        for (Question q : selectedQuestions) {
            boolean duplicate =
                    questions.values().stream()
                            .anyMatch(old -> old.text().equalsIgnoreCase(q.text().trim()));
            if (!duplicate) {
                questions.put(q.id(), q);
                added++;
            }
        }
        return added;
    }

    public List<Question> getBank() {
        return List.copyOf(questions.values());
    }

    public synchronized List<Question> owned(author.User u) {
        return questions.values().stream()
                .filter(q -> u.getEmail().equals(owners.get(q.id())))
                .toList();
    }

    public synchronized List<Question> active(author.User u) {
        return owned(u).stream().filter(q -> !archived.containsKey(q.id())).toList();
    }

    public synchronized int addSelected(author.User u, List<Question> qs) {
        if (u.getRole() != author.Role.TEACHER)
            throw new IllegalArgumentException("Chỉ giáo viên được quản lý ngân hàng.");
        int count = 0;
        for (Question q : qs)
            if (owned(u).stream().noneMatch(old -> old.text().equalsIgnoreCase(q.text().trim()))) {
                questions.put(q.id(), q);
                owners.put(q.id(), u.getEmail());
                count++;
            }
        return count;
    }

    public synchronized List<Question> active() {
        return questions.values().stream().filter(q -> !archived.containsKey(q.id())).toList();
    }

    public synchronized boolean archived(String id) {
        return archived.containsKey(id);
    }

    public synchronized void archive(String id, boolean value) {
        if (questions.values().stream().noneMatch(q -> q.id().equals(id)))
            throw new IllegalArgumentException("Không tìm thấy câu hỏi.");
        if (value) archived.put(id, true);
        else archived.remove(id);
    }

    public synchronized void update(Question q) {
        if (questions.containsKey(q.id())) {
            questions.put(q.id(), q);
            return;
        }
        throw new IllegalArgumentException("Không tìm thấy câu hỏi.");
    }
}
