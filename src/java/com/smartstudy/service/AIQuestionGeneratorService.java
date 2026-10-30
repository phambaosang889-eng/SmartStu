package com.smartstudy.service;

import com.smartstudy.model.*;
import java.util.*;

/** A curated template provider, explicitly not a connected AI model. */
public final class AIQuestionGeneratorService {
    public List<Question> generate(AIQuestionRequest r, int offset) {
        List<Question> out = new ArrayList<>();
        for (int i = 0; i < r.count(); i++) {
            int bias = r.hardness().equals("Khó") ? 5 : r.hardness().equals("Dễ") ? 0 : 2;
            int n = 2 + Math.floorMod(i + offset + bias, 20), v = n + 1, answer;
            String text, explanation, unit;
            List<String> options;
            int correct = 1;
            if (!r.level().startsWith("Vận dụng")) {
                String[][] concepts =
                        switch (r.topic()) {
                            case "Chuyển động" ->
                                    new String[][] {
                                        {
                                            "Gia tốc đặc trưng cho đại lượng nào?",
                                            "Quãng đường đi được",
                                            "Sự thay đổi vận tốc theo thời gian",
                                            "Khối lượng của vật",
                                            "Thời gian chuyển động",
                                            "Gia tốc a = Δv/Δt đặc trưng cho tốc độ biến thiên của"
                                                + " vận tốc."
                                        },
                                        {
                                            "Trong chuyển động thẳng đều, gia tốc bằng bao nhiêu?",
                                            "Bằng vận tốc",
                                            "Bằng 0",
                                            "Luôn dương",
                                            "Luôn âm",
                                            "Vận tốc không đổi nên Δv = 0, do đó a = 0."
                                        }
                                    };
                            case "Năng lượng" ->
                                    new String[][] {
                                        {
                                            "Đơn vị SI của công là gì?",
                                            "Newton",
                                            "Joule",
                                            "Watt",
                                            "Kilogram",
                                            "Công W = Fs cos α có đơn vị N·m, gọi là joule (J)."
                                        },
                                        {
                                            "Khi vận tốc tăng gấp đôi, động năng thay đổi thế nào?",
                                            "Tăng gấp đôi",
                                            "Tăng gấp bốn",
                                            "Giảm một nửa",
                                            "Không đổi",
                                            "Wđ = ½mv². Với m không đổi, v tăng 2 lần làm v² tăng 4"
                                                + " lần."
                                        }
                                    };
                            default ->
                                    new String[][] {
                                        {
                                            "Khi hợp lực bằng 0, vật đang chuyển động sẽ thế nào?",
                                            "Dừng ngay",
                                            "Tiếp tục chuyển động thẳng đều",
                                            "Tăng tốc",
                                            "Đổi hướng",
                                            "Định luật I Newton: hợp lực bằng 0 thì vận tốc không"
                                                + " đổi."
                                        },
                                        {
                                            "Lực và phản lực trong định luật III Newton đặt lên"
                                                + " đâu?",
                                            "Cùng một vật",
                                            "Hai vật khác nhau",
                                            "Chỉ lên vật nặng hơn",
                                            "Không tác dụng lên vật",
                                            "Hai lực cùng độ lớn, ngược chiều và đặt lên hai vật"
                                                + " tương tác khác nhau."
                                        }
                                    };
                        };
                String[] c = concepts[Math.floorMod(i + offset, concepts.length)];
                text = c[0];
                options = List.of(c[1], c[2], c[3], c[4]);
                explanation = c[5];
            } else {
                switch (r.topic()) {
                    case "Chuyển động" -> {
                        boolean high = r.level().equals("Vận dụng cao");
                        int initial = high ? n : 0;
                        answer = initial + n * v;
                        unit = "m/s";
                        text =
                                "Vật có vận tốc đầu "
                                        + initial
                                        + " m/s theo chiều dương, chuyển động thẳng với gia tốc "
                                        + n
                                        + " m/s² trong "
                                        + v
                                        + " s. Vận tốc cuối bằng bao nhiêu?";
                        explanation =
                                "v = v₀ + at = "
                                        + initial
                                        + " + "
                                        + n
                                        + " × "
                                        + v
                                        + " = "
                                        + answer
                                        + " m/s, theo chiều dương.";
                    }
                    case "Năng lượng" -> {
                        boolean high = r.level().equals("Vận dụng cao");
                        answer = n * v;
                        unit = "J";
                        text =
                                high
                                        ? "Lực không đổi "
                                                + (2 * n)
                                                + " N hợp với độ dời "
                                                + v
                                                + " m một góc 60°. Công của lực bằng bao nhiêu?"
                                        : "Lực không đổi "
                                                + n
                                                + " N cùng hướng độ dời "
                                                + v
                                                + " m. Công của lực bằng bao nhiêu?";
                        explanation =
                                high
                                        ? "W = Fs cos 60° = "
                                                + (2 * n)
                                                + " × "
                                                + v
                                                + " × 0,5 = "
                                                + answer
                                                + " J."
                                        : "W = Fs cos 0° = "
                                                + n
                                                + " × "
                                                + v
                                                + " = "
                                                + answer
                                                + " J.";
                    }
                    default -> {
                        boolean high = r.level().equals("Vận dụng cao");
                        answer = v;
                        unit = "m/s²";
                        text =
                                high
                                        ? "Vật "
                                                + n
                                                + " kg chịu lực kéo ngang "
                                                + (n * v + 2 * n)
                                                + " N và lực cản ngược chiều "
                                                + (2 * n)
                                                + " N. Gia tốc theo chiều kéo bằng bao nhiêu?"
                                        : "Vật khối lượng "
                                                + n
                                                + " kg chịu hợp lực "
                                                + (n * v)
                                                + " N. Gia tốc bằng bao nhiêu?";
                        explanation =
                                (high
                                                ? "Hợp lực F = "
                                                        + (n * v + 2 * n)
                                                        + " − "
                                                        + (2 * n)
                                                        + " = "
                                                        + (n * v)
                                                        + " N. "
                                                : "")
                                        + "a = F/m = "
                                        + (n * v)
                                        + "/"
                                        + n
                                        + " = "
                                        + v
                                        + " m/s², cùng hướng hợp lực.";
                    }
                }
                options =
                        List.of(
                                (answer - 1) + " " + unit,
                                answer + " " + unit,
                                (answer + 1) + " " + unit,
                                (answer + 2) + " " + unit);
            }
            // Rotate positions deterministically so the correct choice is not always B.
            List<String> rotated = new ArrayList<>(options);
            int shift = Math.floorMod(i + offset, 4);
            Collections.rotate(rotated, shift);
            correct = (correct + shift) % 4;
            Question q =
                    new Question(
                            null,
                            text,
                            rotated,
                            correct,
                            explanation,
                            r.topic(),
                            r.level(),
                            r.points(),
                            "Dữ liệu mẫu — không phải AI thật",
                            r.lesson(),
                            r.objective(),
                            r.hardness(),
                            r.seconds());
            if (out.stream().noneMatch(old -> old.text().equals(q.text()))) out.add(q);
        }
        return List.copyOf(out);
    }
}
