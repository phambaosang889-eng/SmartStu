package com.smartstudy.model;

import java.util.List;

public record QuizDefinition(
        String id,
        String title,
        String description,
        int minutes,
        String status,
        String owner,
        List<Question> questions,
        int version,
        java.time.LocalDateTime due,
        int maxAttempts,
        int maxScore) {
    public QuizDefinition {
        questions = List.copyOf(questions);
    }

    public QuizDefinition(
            String id,
            String title,
            String description,
            int minutes,
            String status,
            String owner,
            List<Question> questions,
            int version) {
        this(id, title, description, minutes, status, owner, questions, version, null, 0, 100);
    }
}
