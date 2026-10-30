package com.smartstudy.service;

import com.smartstudy.model.EssayResult;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Deterministic, explicitly non-AI sample rubric for the in-memory demo. */
public final class EssayService {
    private static final EssayService INSTANCE = new EssayService();
    private final List<EssayResult> submissions = new CopyOnWriteArrayList<>();

    private EssayService() {}

    public static EssayService getInstance() {
        return INSTANCE;
    }

    public EssayResult submit(String email, String answer) {
        if (answer == null || answer.trim().length() < 20)
            throw new IllegalArgumentException("Câu trả lời cần ít nhất 20 ký tự.");
        if (answer.length() > 3000)
            throw new IllegalArgumentException("Câu trả lời vượt quá 3.000 ký tự.");
        String a = answer.toLowerCase().replace(" ", "").replace("²", "2").replace("^", "");
        boolean formula = a.contains("f=ma") || a.contains("a=f/m");
        boolean values = a.contains("10n") && a.contains("2kg");
        boolean calculation = a.contains("10/2") || a.contains("5m/s2") || a.contains("5m/s");
        boolean explanation =
                a.contains("gia tốc") || a.contains("gia toc") || a.contains("newton");
        double score =
                (formula ? 2.5 : 0)
                        + (values ? 2.0 : 0)
                        + (calculation ? 3.0 : 0)
                        + (explanation ? 2.5 : 0);
        List<String> criteria =
                List.of(
                        "Công thức định luật II Newton: "
                                + (formula ? "đạt (2,5/2,5)" : "cần bổ sung (0/2,5)"),
                        "Xác định dữ kiện F = 10 N, m = 2 kg: "
                                + (values ? "đạt (2/2)" : "cần nêu rõ (0/2)"),
                        "Phép tính a = F/m: " + (calculation ? "đạt (3/3)" : "cần trình bày (0/3)"),
                        "Giải thích kết quả: "
                                + (explanation ? "đạt (2,5/2,5)" : "cần giải thích thêm (0/2,5)"));
        EssayResult result =
                new EssayResult(
                        email,
                        answer,
                        score,
                        criteria,
                        "Cần giáo viên xem lại — chấm mô phỏng",
                        Instant.now());
        submissions.add(0, result);
        return result;
    }

    public List<EssayResult> getSubmissions() {
        return List.copyOf(submissions);
    }
}
