package com.smartstudy.model;

import java.time.Instant;
import java.util.List;

public record EssayGradingResult(
        List<Double> scores,
        List<String> comments,
        List<String> evidence,
        String strengths,
        String errors,
        String improvements,
        double confidence,
        Instant at) {
    public EssayGradingResult {
        scores = List.copyOf(scores);
        comments = List.copyOf(comments);
        evidence = List.copyOf(evidence);
    }

    public double total() {
        return scores.stream().mapToDouble(Double::doubleValue).sum();
    }

    public String feedback() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < comments.size(); i++)
            s.append("Tiêu chí ")
                    .append(i + 1)
                    .append(": ")
                    .append(comments.get(i))
                    .append("\nDẫn chiếu: ")
                    .append(evidence.get(i))
                    .append("\n\n");
        return s
                + "Ưu điểm: "
                + strengths
                + "\nThiếu sót: "
                + errors
                + "\nCải thiện: "
                + improvements;
    }
}
