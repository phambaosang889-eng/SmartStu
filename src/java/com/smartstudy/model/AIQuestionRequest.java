package com.smartstudy.model;

import java.util.Set;

public record AIQuestionRequest(
        String topic,
        int count,
        String level,
        int points,
        String lesson,
        String objective,
        String hardness,
        int seconds,
        String extra) {
    public AIQuestionRequest(String topic, int count, String level, int points) {
        this(
                topic,
                count,
                level,
                points,
                "Vật lý 10",
                "Vận dụng kiến thức đúng, ghi rõ đơn vị SI",
                "Trung bình",
                60,
                "");
    }

    public AIQuestionRequest {
        if (topic == null
                || level == null
                || !Set.of("Định luật Newton", "Chuyển động", "Năng lượng").contains(topic)
                || count < 1
                || count > 10
                || !Set.of("Nhận biết", "Thông hiểu", "Vận dụng", "Vận dụng cao").contains(level)
                || points < 1
                || points > 10)
            throw new IllegalArgumentException(
                    "Chọn chủ đề, mức độ hợp lệ; từ 1–10 câu và 1–10 điểm mỗi câu.");
        if (lesson == null
                || lesson.isBlank()
                || lesson.length() > 200
                || objective == null
                || objective.isBlank()
                || objective.length() > 600
                || hardness == null
                || !Set.of("Dễ", "Trung bình", "Khó").contains(hardness)
                || seconds < 15
                || seconds > 600
                || extra == null
                || extra.length() > 1500)
            throw new IllegalArgumentException(
                    "Kiểm tra phạm vi bài học, mục tiêu, độ khó, thời gian 15–600 giây và yêu cầu"
                        + " bổ sung.");
    }
}
