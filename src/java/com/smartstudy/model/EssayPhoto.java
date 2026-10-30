package com.smartstudy.model;

import java.time.Instant;

/** Image bytes are fetched separately, never included in JSP metadata. */
public record EssayPhoto(
        String id,
        String assignment,
        String student,
        String submission,
        int bytes,
        int width,
        int height,
        String sha256,
        String transcription,
        String warning,
        Double confidence,
        Instant created) {}
