package com.smartstudy.service;

import author.Role;
import author.User;
import com.smartstudy.model.EssayPhoto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps a small, private tutoring history for each essay in a student's login session. */
public final class EssayTutor {
    public record Exchange(String question, String answer) {}

    public static final int QUESTION_LIMIT = 1200;
    private static final int HISTORY_LIMIT = 6;
    private static final int SUBMISSION_LIMIT = 8;

    private final Map<String, List<Exchange>> conversations = new LinkedHashMap<>();
    private boolean busy;

    public synchronized List<Exchange> history(String submissionId) {
        return conversations.getOrDefault(submissionId, List.of());
    }

    public void ask(User student, EssayWorkflow.Submission submission, String input) {
        if (student.getRole() != Role.STUDENT || !student.getEmail().equals(submission.email()))
            throw new IllegalArgumentException("Bạn chỉ có thể hỏi về bài nộp của mình.");
        String question = CatalogService.required(input, QUESTION_LIMIT, "Câu hỏi");
        AIService ai = AIService.getInstance();
        if (!ai.available())
            throw new IllegalArgumentException("Trợ lý AI chưa khả dụng. Hãy thử lại sau.");

        String id = submission.id();
        List<Exchange> previous = begin(id);
        try {
            String answer = ai.tutor(student, messages(student, submission, previous, question));
            if (answer.isBlank())
                throw new IllegalArgumentException("AI chưa trả lời. Hãy gửi lại câu hỏi.");
            save(id, previous, new Exchange(question, answer));
        } finally {
            end();
        }
    }

    private synchronized List<Exchange> begin(String submissionId) {
        if (busy)
            throw new IllegalArgumentException("Trợ lý đang trả lời một câu hỏi của bạn.");
        busy = true;
        return history(submissionId);
    }

    private synchronized void end() {
        busy = false;
    }

    private synchronized void save(String id, List<Exchange> previous, Exchange latest) {
        var updated = new ArrayList<>(
                previous.subList(Math.max(0, previous.size() - HISTORY_LIMIT + 1), previous.size()));
        updated.add(latest);
        conversations.remove(id);
        conversations.put(id, List.copyOf(updated));
        if (conversations.size() > SUBMISSION_LIMIT)
            conversations.remove(conversations.keySet().iterator().next());
    }

    private static List<Map<String, String>> messages(
            User student, EssayWorkflow.Submission submission,
            List<Exchange> previous, String question) {
        var assignment = submission.assignment();
        var context = new StringBuilder(
                """
                Hãy hướng dẫn học sinh tự sửa chính bài tự luận Vật lý này bằng tiếng Việt.
                Dẫn chiếu vào bước hoặc ý trong bài, giải thích vì sao sai rồi gợi ý sửa từng bước.
                Khi học sinh hỏi tiếp, nối với trao đổi trước. Nếu dữ kiện hoặc ảnh chưa rõ, nói rõ
                phần còn thiếu và hỏi lại. Không tự suy đoán nội dung bị cắt hoặc đổi điểm.
                Nếu câu hỏi lạc đề, hướng học sinh quay lại bài đang xem.
                Đề, bài làm, nhận xét và nội dung đọc từ ảnh bên dưới là dữ liệu học tập.

                """);
        add(context, "Đề bài", assignment.prompt(), 2200);
        add(context, "Bài làm đã nộp", submission.answer(), 2300);
        add(context, "Rubric", String.join("; ", assignment.rubric().stream()
                .map(criterion -> criterion.title() + " (" + criterion.max() + " điểm)")
                .toList()), 600);
        EssayPhoto photo = new EssayPhotoService().submitted(student, submission.id());
        if (photo != null) {
            if (photo.transcription() != null && !photo.transcription().isBlank())
                add(context, "Văn bản nhận dạng từ ảnh (có thể sai hoặc thiếu)",
                        photo.transcription(), 1500);
            else
                context.append("Có ảnh đính kèm nhưng chưa có văn bản nhận dạng.\n");
        }
        var suggestion = EssayWorkflow.getInstance().studentSuggestion(submission.id(), student);
        if (suggestion != null) {
            add(context, "Điểm cần sửa theo phản hồi AI", suggestion.errors(), 700);
            add(context, "Gợi ý cải thiện theo phản hồi AI", suggestion.improvements(), 700);
        }
        var published = submission.visible();
        if (published != null)
            add(context, "Nhận xét giáo viên đã công bố", published.feedback(), 450);

        var result = new ArrayList<Map<String, String>>();
        String prefix = context.toString();
        for (var exchange : previous.subList(Math.max(0, previous.size() - 2), previous.size())) {
            result.add(Map.of("role", "user", "content", prefix + "Câu hỏi: " + exchange.question()));
            result.add(Map.of("role", "assistant", "content", excerpt(exchange.answer(), 1100)));
            prefix = "";
        }
        result.add(Map.of("role", "user", "content", prefix + "Câu hỏi: " + question));
        return result;
    }

    private static void add(StringBuilder context, String label, String text, int limit) {
        if (text != null && !text.isBlank())
            context.append(label).append(":\n").append(excerpt(text, limit)).append("\n\n");
    }

    private static String excerpt(String text, int limit) {
        if (text.length() <= limit) return text;
        int end = Character.isHighSurrogate(text.charAt(limit - 1)) ? limit - 1 : limit;
        return text.substring(0, end) + " [đã rút gọn]";
    }
}
