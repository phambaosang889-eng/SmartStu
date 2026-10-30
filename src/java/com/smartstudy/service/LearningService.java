package com.smartstudy.service;

import java.time.Instant;
import java.util.*;

public final class LearningService {
    public record Activity(String title, String detail, Instant time) {}

    private static final LearningService INSTANCE = new LearningService();
    private final Map<String, Set<String>> completed =
            com.smartstudy.persistence.Stores.map(
                    "lesson_progress",
                    new com.smartstudy.persistence.RecordCodec.TypeRef<Set<String>>() {}.type());
    private final Map<String, List<Activity>> activities =
            com.smartstudy.persistence.Stores.map(
                    "learning_activities",
                    new com.smartstudy.persistence.RecordCodec.TypeRef<List<Activity>>() {}.type());
    private final Map<String, List<Integer>> scores =
            com.smartstudy.persistence.Stores.map(
                    "learning_scores",
                    new com.smartstudy.persistence.RecordCodec.TypeRef<List<Integer>>() {}.type());
    private final Map<String, Set<String>> cards =
            com.smartstudy.persistence.Stores.map(
                    "flashcard_progress",
                    new com.smartstudy.persistence.RecordCodec.TypeRef<Set<String>>() {}.type());
    private final Map<String, Instant> pulses = new HashMap<>();
    private final Map<String, Long> seconds =
            com.smartstudy.persistence.Stores.map("learning_time", Long.class);
    private final Map<String, String> lastLesson =
            com.smartstudy.persistence.Stores.map("last_lesson", String.class);

    public synchronized void read(String email, String id) {
        pulses.put(email + ":" + id, Instant.now());
        lastLesson.put(email, id);
    }

    public synchronized void pulse(String email, String id) {
        String key = email + ":" + id;
        Instant before = pulses.get(key), now = Instant.now();
        if (before != null) {
            long elapsed = java.time.Duration.between(before, now).getSeconds();
            if (elapsed >= 0 && elapsed <= 45) seconds.merge(email, elapsed, Long::sum);
        }
        pulses.put(key, now);
    }

    public synchronized long minutes(String email) {
        return seconds.getOrDefault(email, 0L) / 60;
    }

    public synchronized String lastLesson(String email) {
        return lastLesson.get(email);
    }

    public synchronized int streak(String email) {
        var days =
                activities(email).stream()
                        .map(
                                a ->
                                        a.time()
                                                .atZone(java.time.ZoneId.of("Asia/Bangkok"))
                                                .toLocalDate())
                        .collect(java.util.stream.Collectors.toSet());
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Bangkok"));
        if (!days.contains(today)) today = today.minusDays(1);
        int n = 0;
        while (days.contains(today)) {
            n++;
            today = today.minusDays(1);
        }
        return n;
    }

    public static LearningService getInstance() {
        return INSTANCE;
    }

    public synchronized void complete(String email, String id, String title) {
        Set<String> done = new LinkedHashSet<>(completed(email));
        if (done.add(id)) {
            completed.put(email, done);
            log(email, "Hoàn thành bài học", title);
        }
    }

    public synchronized Set<String> completed(String email) {
        return Set.copyOf(completed.getOrDefault(email, Set.of()));
    }

    public synchronized void quiz(String email, int score) {
        List<Integer> updated = new ArrayList<>(scores.getOrDefault(email, List.of()));
        updated.add(score);
        scores.put(email, updated);
        log(email, "Nộp quiz", score + " / 100 điểm");
    }

    public synchronized void log(String email, String title, String detail) {
        List<Activity> a = new ArrayList<>(activities.getOrDefault(email, List.of()));
        a.add(0, new Activity(title, detail, Instant.now()));
        if (a.size() > 200) a.remove(a.size() - 1);
        activities.put(email, a);
    }

    public synchronized List<Activity> activities(String email) {
        return List.copyOf(activities.getOrDefault(email, List.of()));
    }

    public synchronized int average(String email) {
        return (int)
                Math.round(
                        scores.getOrDefault(email, List.of()).stream()
                                .mapToInt(Integer::intValue)
                                .average()
                                .orElse(0));
    }

    public synchronized int attempts(String email) {
        return scores.getOrDefault(email, List.of()).size();
    }

    public synchronized int totalAttempts() {
        return scores.values().stream().mapToInt(List::size).sum();
    }

    public synchronized int totalCompleted() {
        return completed.values().stream().mapToInt(Set::size).sum();
    }

    public synchronized Set<String> known(String email) {
        return Set.copyOf(cards.getOrDefault(email, Set.of()));
    }

    public synchronized void mark(String email, String id, boolean known) {
        Set<String> set = new HashSet<>(known(email));
        if (known) set.add(id);
        else set.remove(id);
        cards.put(email, set);
    }
}
