package com.smartstudy.model;

import java.time.Instant;

public record LessonVideo(
        String id,
        String lesson,
        String owner,
        String title,
        String type,
        String url,
        String source,
        String storage,
        String mime,
        long bytes,
        boolean archived,
        Instant createdAt) {}
