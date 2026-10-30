package com.smartstudy.service;

import author.*;
import com.smartstudy.model.CatalogEntry;
import java.time.*;
import java.util.*;

/** Relationships shared by learning, assessment and administration. */
public final class CourseService {
    public record Chapter(String id, String course, String title, int order) {}

    public record Placement(String course, String chapter, int order, String resourceUrl) {}

    public record Enrollment(String email, String course, Instant at, boolean active) {}

    private static final CourseService INSTANCE = new CourseService();

    public static CourseService getInstance() {
        return INSTANCE;
    }

    private final Map<String, Chapter> chapters =
            com.smartstudy.persistence.Stores.map("chapters", Chapter.class);
    private final Map<String, Placement> placements =
            com.smartstudy.persistence.Stores.map("content_placements", Placement.class);
    private final Map<String, Enrollment> enrollments =
            com.smartstudy.persistence.Stores.map("enrollments", Enrollment.class);
    private final Map<String, String> covers =
            com.smartstudy.persistence.Stores.map("course_covers", String.class);

    public synchronized String cover(String id) {
        return covers.getOrDefault(id, "");
    }

    public String validateCover(String url) {
        url = url == null ? "" : url.trim();
        if (!url.isBlank()) {
            try {
                var uri = java.net.URI.create(url);
                if (!"https".equals(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null
                        || url.length() > 1000) throw new IllegalArgumentException();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Ảnh đại diện cần đường dẫn HTTPS hợp lệ, tối đa 1.000 ký tự.");
            }
        }
        return url;
    }

    public synchronized void cover(User u, String id, String url) {
        if (!owns(u, id)) throw new IllegalArgumentException("Không có quyền sửa ảnh khóa học.");
        covers.put(id, validateCover(url));
    }

    private CourseService() {
        if (!com.smartstudy.persistence.Stores.seed() || !chapters.isEmpty()) return;
        chapters.put(
                "chapter-motion",
                new Chapter("chapter-motion", "course-mechanics", "Chuyển động", 1));
        chapters.put(
                "chapter-force",
                new Chapter("chapter-force", "course-mechanics", "Lực và năng lượng", 2));
        String[] ids = {
            "lesson-motion",
            "lesson-newton",
            "lesson-friction",
            "lesson-energy",
            "card-newton",
            "card-speed",
            "card-work",
            "newton-demo",
            "newton",
            "notice-review",
            "resource-guide"
        };
        int i = 0;
        for (String id : ids)
            placements.put(
                    id,
                    new Placement(
                            "course-mechanics",
                            id.equals("lesson-motion") ? "chapter-motion" : "chapter-force",
                            ++i,
                            ""));
        enrollments.put(
                "student@smartstudy.vn:course-mechanics",
                new Enrollment("student@smartstudy.vn", "course-mechanics", Instant.now(), true));
    }

    public boolean owns(User u, String course) {
        CatalogEntry c = CatalogService.getInstance().get(course);
        return c != null
                && c.kind().equals("courses")
                && c.owner().equals(u.getEmail())
                && u.getRole() == Role.TEACHER;
    }

    public synchronized Placement placement(String id) {
        return placements.get(id);
    }

    public synchronized String courseOf(String id) {
        Placement p = placements.get(id);
        return p == null ? "" : p.course();
    }

    public synchronized boolean enrolled(String email, String course) {
        Enrollment e = enrollments.get(email + ":" + course);
        return e != null && e.active();
    }

    public boolean canLearn(User u, String id) {
        String course = courseOf(id);
        CatalogEntry c = CatalogService.getInstance().get(course);
        return u.getRole() == Role.STUDENT
                && c != null
                && c.status().equals("published")
                && enrolled(u.getEmail(), course);
    }

    public List<CatalogEntry> courses(User u) {
        return CatalogService.getInstance().list("courses", u).stream()
                .filter(c -> u.getRole() != Role.STUDENT || c.status().equals("published"))
                .toList();
    }

    public synchronized List<Chapter> chapters(String course) {
        return chapters.values().stream()
                .filter(c -> c.course().equals(course))
                .sorted(Comparator.comparingInt(Chapter::order))
                .toList();
    }

    public synchronized Chapter chapter(String id) {
        return chapters.get(id);
    }

    public synchronized List<Chapter> availableChapters(User u) {
        return chapters.values().stream()
                .filter(c -> owns(u, c.course()))
                .sorted(Comparator.comparing(Chapter::course).thenComparingInt(Chapter::order))
                .toList();
    }

    public synchronized Placement validate(User u, String chapter, int order, String url) {
        Chapter ch = chapters.get(chapter);
        if (ch == null || !owns(u, ch.course()))
            throw new IllegalArgumentException(
                    "Chọn chương trong khóa học do bạn phụ trách. Nếu chưa có chương, tạo chương ở"
                        + " trang khóa học trước.");
        if (order < 1 || order > 999) throw new IllegalArgumentException("Thứ tự từ 1–999.");
        url = url == null ? "" : url.trim();
        if (!url.isBlank()) {
            try {
                var uri = java.net.URI.create(url);
                if (!"https".equals(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null) throw new IllegalArgumentException();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Đường dẫn tài liệu/video phải là HTTPS hợp lệ.");
            }
            if (url.length() > 1000) throw new IllegalArgumentException("Đường dẫn quá dài.");
        }
        return new Placement(ch.course(), chapter, order, url);
    }

    public synchronized void place(String id, Placement p) {
        placements.put(id, p);
    }

    public synchronized void chapter(User u, String course, String id, String title, int order) {
        if (!owns(u, course))
            throw new IllegalArgumentException("Bạn không phụ trách khóa học này.");
        title = CatalogService.required(title, 150, "Tên chương");
        if (order < 1 || order > 999) throw new IllegalArgumentException("Thứ tự từ 1–999.");
        Chapter old = chapters.get(id);
        if (id != null && !id.isBlank() && (old == null || !old.course().equals(course)))
            throw new IllegalArgumentException("Chương không thuộc khóa học.");
        String key = old == null ? UUID.randomUUID().toString() : old.id();
        chapters.put(key, new Chapter(key, course, title, order));
    }

    public synchronized void removeChapter(User u, String course, String id) {
        if (!owns(u, course)) throw new IllegalArgumentException("Không có quyền.");
        Chapter c = chapters.get(id);
        if (c == null || !c.course().equals(course))
            throw new IllegalArgumentException("Chương không tồn tại.");
        if (placements.values().stream().anyMatch(p -> p.chapter().equals(id)))
            throw new IllegalArgumentException(
                    "Chuyển học liệu sang chương khác trước khi xóa chương.");
        chapters.remove(id);
    }

    public synchronized void place(
            User u, String id, String course, String chapter, int order, String url) {
        if (!owns(u, course)) throw new IllegalArgumentException("Chọn khóa học do bạn phụ trách.");
        Chapter c = chapters.get(chapter);
        if (c == null || !c.course().equals(course))
            throw new IllegalArgumentException("Chọn chương thuộc khóa học đã chọn.");
        if (order < 1 || order > 999)
            throw new IllegalArgumentException("Thứ tự học liệu từ 1–999.");
        url = url == null ? "" : url.trim();
        if (!url.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(url);
                if (!"https".equals(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null) throw new IllegalArgumentException();
            } catch (Exception e) {
                throw new IllegalArgumentException("Tài liệu/video cần đường dẫn HTTPS hợp lệ.");
            }
            if (url.length() > 1000) throw new IllegalArgumentException("Đường dẫn quá dài.");
        }
        placements.put(id, new Placement(course, chapter, order, url));
    }

    public synchronized void enroll(User u, String course, boolean active) {
        CatalogEntry c = CatalogService.getInstance().get(course);
        if (u.getRole() != Role.STUDENT
                || c == null
                || !c.kind().equals("courses")
                || !c.status().equals("published"))
            throw new IllegalArgumentException("Khóa học không mở đăng ký.");
        String key = u.getEmail() + ":" + course;
        Enrollment old = enrollments.get(key);
        enrollments.put(
                key,
                new Enrollment(
                        u.getEmail(), course, old == null ? Instant.now() : old.at(), active));
        LearningService.getInstance()
                .log(
                        u.getEmail(),
                        active ? "Đăng ký khóa học" : "Dừng tham gia khóa học",
                        c.title());
    }

    public List<CatalogEntry> contents(User u, String kind) {
        List<CatalogEntry> all =
                u.getRole() == Role.STUDENT
                        ? CatalogService.getInstance().published(kind)
                        : CatalogService.getInstance().list(kind, u);
        return all.stream()
                .filter(e -> u.getRole() != Role.STUDENT || canLearn(u, e.id()))
                .sorted(
                        Comparator.comparingInt(
                                e -> {
                                    Placement p = placement(e.id());
                                    return p == null ? 999 : p.order();
                                }))
                .toList();
    }

    public List<CatalogEntry> lessons(String course) {
        return CatalogService.getInstance().published("lesson").stream()
                .filter(e -> courseOf(e.id()).equals(course))
                .sorted(Comparator.comparingInt(e -> placement(e.id()).order()))
                .toList();
    }

    public synchronized List<Enrollment> enrollments(User u) {
        return enrollments.values().stream()
                .filter(
                        e ->
                                u.getRole() == Role.ADMIN
                                        || u.getRole() == Role.STUDENT
                                                && e.email().equals(u.getEmail())
                                        || owns(u, e.course()))
                .toList();
    }

    public synchronized Set<String> students(User teacher) {
        Set<String> emails = new LinkedHashSet<>();
        for (Enrollment e : enrollments(teacher)) if (e.active()) emails.add(e.email());
        return Set.copyOf(emails);
    }

    public synchronized void assignAssessment(User u, String id, String course) {
        if (!owns(u, course)) throw new IllegalArgumentException("Chọn khóa học do bạn phụ trách.");
        placements.put(id, new Placement(course, "", 1, ""));
    }

    public double progress(String email, String course) {
        List<CatalogEntry> lessons = lessons(course);
        Set<String> done = LearningService.getInstance().completed(email);
        return lessons.isEmpty()
                ? 0
                : 100.0
                        * lessons.stream().filter(l -> done.contains(l.id())).count()
                        / lessons.size();
    }
}
