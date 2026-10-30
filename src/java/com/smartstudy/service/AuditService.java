package com.smartstudy.service;

import java.time.Instant;
import java.util.*;

public final class AuditService {
    public record Event(Instant time, String actor, String action, String target) {}

    private static final Map<String, Event> EVENTS =
            com.smartstudy.persistence.Stores.map("audit_events", Event.class);

    private AuditService() {}

    public static synchronized void record(String actor, String action, String target) {
        EVENTS.put(UUID.randomUUID().toString(), new Event(Instant.now(), actor, action, target));
    }

    public static synchronized List<Event> events() {
        return (EVENTS instanceof com.smartstudy.dao.JdbcMap<Event> jdbc
                ? jdbc.latest(1000)
                : EVENTS.values().stream()
                        .sorted(Comparator.comparing(Event::time).reversed())
                        .limit(1000)
                        .toList());
    }
}
