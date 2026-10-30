package com.smartstudy.service;

import author.*;
import com.smartstudy.model.*;
import java.time.*;
import java.util.*;

public final class ReportingService {
    public record CourseRow(
            String id,
            String title,
            String owner,
            long enrolled,
            long completed,
            int percent,
            String average,
            long pending) {}

    public record DayRow(LocalDate day, long count) {}

    public record TopicRow(String topic, int wrong, int total) {}

    private ReportingService() {}

    public static List<CourseRow> courses(User u, String selected, String teacher) {
        return courses(u, selected, teacher, null, null);
    }

    private static boolean between(Instant at, LocalDate from, LocalDate to) {
        LocalDate d = at.atZone(ZoneId.of("Asia/Bangkok")).toLocalDate();
        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
    }

    public static List<CourseRow> courses(
            User u, String selected, String teacher, LocalDate from, LocalDate to) {
        CourseService cs = CourseService.getInstance();
        var attempts = QuizService.getInstance().history(u);
        var essays = EssayWorkflow.getInstance().monitoring(u);
        List<CourseRow> rows = new ArrayList<>();
        for (CatalogEntry c : cs.courses(u)) {
            if (selected != null && !selected.isBlank() && !selected.equals(c.id())
                    || teacher != null && !teacher.isBlank() && !teacher.equals(c.owner()))
                continue;
            var enrolled =
                    cs.enrollments(u).stream()
                            .filter(
                                    e ->
                                            e.course().equals(c.id())
                                                    && e.active()
                                                    && between(e.at(), from, to))
                            .toList();
            long done =
                    enrolled.stream().filter(e -> cs.progress(e.email(), c.id()) >= 100).count();
            int percent =
                    (int)
                            Math.round(
                                    enrolled.stream()
                                            .mapToDouble(e -> cs.progress(e.email(), c.id()))
                                            .average()
                                            .orElse(0));
            var results =
                    attempts.stream()
                            .filter(
                                    r ->
                                            cs.courseOf(r.run().quiz().id()).equals(c.id())
                                                    && between(r.finished(), from, to))
                            .toList();
            String avg =
                    results.isEmpty()
                            ? "Chưa có"
                            : Math.round(
                                            results.stream()
                                                    .mapToDouble(
                                                            r ->
                                                                    r.score()
                                                                            * 100.0
                                                                            / r.run()
                                                                                    .quiz()
                                                                                    .maxScore())
                                                    .average()
                                                    .orElse(0))
                                    + "%";
            long pending =
                    essays.stream()
                            .filter(
                                    s ->
                                            cs.courseOf(s.assignment().id()).equals(c.id())
                                                    && s.visible() == null
                                                    && between(s.at(), from, to))
                            .count();
            rows.add(
                    new CourseRow(
                            c.id(),
                            c.title(),
                            c.owner(),
                            enrolled.size(),
                            done,
                            percent,
                            avg,
                            pending));
        }
        return List.copyOf(rows);
    }

    public static List<LearningService.Activity> activities(User u) {
        if (u.getRole() == Role.STUDENT)
            return LearningService.getInstance().activities(u.getEmail());
        List<LearningService.Activity> list = new ArrayList<>();
        for (var r : QuizService.getInstance().history(u))
            list.add(
                    new LearningService.Activity(
                            "Nộp quiz",
                            r.run().quiz().title()
                                    + " · "
                                    + r.score()
                                    + "/"
                                    + r.run().quiz().maxScore(),
                            r.finished()));
        for (var s : EssayWorkflow.getInstance().monitoring(u))
            list.add(new LearningService.Activity("Nộp tự luận", s.assignment().title(), s.at()));
        for (var e : AuditService.events())
            if (u.getRole() == Role.ADMIN || e.actor().equals(u.getEmail()))
                list.add(new LearningService.Activity(e.action(), e.target(), e.time()));
        list.sort(Comparator.comparing(LearningService.Activity::time).reversed());
        return List.copyOf(list);
    }

    public static List<DayRow> daily(User u, int days) {
        var activity = activities(u);
        List<DayRow> out = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            out.add(
                    new DayRow(
                            day,
                            activity.stream()
                                    .filter(
                                            a ->
                                                    a.time()
                                                            .atZone(ZoneId.of("Asia/Bangkok"))
                                                            .toLocalDate()
                                                            .equals(day))
                                    .count()));
        }
        return List.copyOf(out);
    }

    public static List<DayRow> growth(User u, boolean users) {
        if (u.getRole() != Role.ADMIN)
            throw new IllegalArgumentException(
                    "Chỉ quản trị viên được xem tăng trưởng toàn hệ thống.");
        List<DayRow> out = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Bangkok"));
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long count =
                    users
                            ? UserStore.list().stream()
                                    .filter(a -> between(a.created(), day, day))
                                    .count()
                            : CourseService.getInstance().courses(u).stream()
                                    .filter(c -> between(c.createdAt(), day, day))
                                    .count();
            out.add(new DayRow(day, count));
        }
        return List.copyOf(out);
    }

    public static List<TopicRow> topics(User u) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (var r : QuizService.getInstance().history(u)) {
            int i = 0;
            for (Question q : r.run().quiz().questions()) {
                int[] v = counts.computeIfAbsent(q.topic(), k -> new int[2]);
                if (r.answers().get(i++) != q.correctIndex()) v[0]++;
                v[1]++;
            }
        }
        return counts.entrySet().stream()
                .map(e -> new TopicRow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    private static String csv(String s) {
        if (s == null) s = "";
        if (s.matches("^[=+@-].*")) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public static String csv(List<CourseRow> rows) {
        StringBuilder s =
                new StringBuilder(
                        "\uFEFFKhóa học,Giáo viên,Đang học,Đã hoàn thành,Tiến độ trung bình"
                            + " (%),Điểm quiz trung bình,Tự luận chờ công bố\r\n");
        for (CourseRow r : rows)
            s.append(csv(r.title()))
                    .append(',')
                    .append(csv(r.owner()))
                    .append(',')
                    .append(r.enrolled())
                    .append(',')
                    .append(r.completed())
                    .append(',')
                    .append(r.percent())
                    .append(',')
                    .append(csv(r.average()))
                    .append(',')
                    .append(r.pending())
                    .append("\r\n");
        return s.toString();
    }
}
