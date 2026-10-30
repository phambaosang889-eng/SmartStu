package com.smartstudy.model;

import java.time.Instant;

/** A demo learning resource or roster entry; not an authentication account. */
public record CatalogEntry(
        String id,
        String kind,
        String title,
        String body,
        String topic,
        String status,
        String owner,
        Instant updatedAt,
        int version,
        Instant createdAt) {
    public CatalogEntry(
            String id,
            String kind,
            String title,
            String body,
            String topic,
            String status,
            String owner,
            Instant updatedAt,
            int version) {
        this(id, kind, title, body, topic, status, owner, updatedAt, version, updatedAt);
    }
}
