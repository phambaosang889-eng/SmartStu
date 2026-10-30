package com.smartstudy.model;

import java.time.Instant;
import java.util.List;

public record EssayResult(
        String studentEmail,
        String answer,
        double score,
        List<String> criteria,
        String status,
        Instant submittedAt) {
    public EssayResult {
        criteria = List.copyOf(criteria);
    }
}
