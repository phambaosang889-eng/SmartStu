package com.smartstudy.model;

import java.util.List;
import java.util.UUID;

public record Question(
        String id,
        String text,
        List<String> options,
        int correctIndex,
        String explanation,
        String topic,
        String difficulty,
        int points,
        String source,
        String lesson,
        String objective,
        String hardness,
        int seconds) {
    public Question(
            String id,
            String text,
            List<String> options,
            int correctIndex,
            String explanation,
            String topic,
            String difficulty,
            int points,
            String source) {
        this(
                id,
                text,
                options,
                correctIndex,
                explanation,
                topic,
                difficulty,
                points,
                source,
                "Vật lý 10",
                "Vận dụng kiến thức và đơn vị SI",
                "Trung bình",
                60);
    }

    public Question {
        id = id == null ? UUID.randomUUID().toString() : id;
        options = List.copyOf(options);
        if (text == null
                || text.isBlank()
                || options.size() < 2
                || correctIndex < 0
                || correctIndex >= options.size()
                || points < 1
                || text.length() > 1000
                || explanation == null
                || explanation.length() > 2000
                || options.stream().anyMatch(o -> o == null || o.isBlank() || o.length() > 300)
                || seconds < 15
                || seconds > 600
                || hardness == null
                || !java.util.Set.of("Dễ", "Trung bình", "Khó").contains(hardness)
                || lesson == null
                || lesson.length() > 200
                || objective == null
                || objective.length() > 600) {
            throw new IllegalArgumentException("Câu hỏi hoặc đáp án không hợp lệ.");
        }
    }
}
