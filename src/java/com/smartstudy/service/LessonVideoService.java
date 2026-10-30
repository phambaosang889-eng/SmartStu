package com.smartstudy.service;

import author.*;
import com.smartstudy.dao.LessonVideoDao;
import com.smartstudy.model.*;
import com.smartstudy.persistence.Database;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class LessonVideoService {
    public static final long MAX_BYTES = 200L * 1024 * 1024;
    private final LessonVideoDao dao = new LessonVideoDao();

    public CatalogEntry requireOwner(User user, String lesson) {
        var entry = CatalogService.getInstance().get(lesson);
        if (user == null
                || user.getRole() != Role.TEACHER
                || entry == null
                || !entry.kind().equals("lesson")
                || !entry.owner().equals(user.getEmail()))
            throw new IllegalArgumentException("Bài học không thuộc phạm vi giảng dạy của bạn.");
        return entry;
    }

    public boolean mayRead(User user, String lesson) {
        var entry = CatalogService.getInstance().get(lesson);
        return user != null
                && entry != null
                && entry.kind().equals("lesson")
                && (user.getRole() == Role.ADMIN
                        || user.getRole() == Role.TEACHER && entry.owner().equals(user.getEmail())
                        || user.getRole() == Role.STUDENT
                                && entry.status().equals("published")
                                && CourseService.getInstance().canLearn(user, lesson));
    }

    public List<LessonVideo> list(User user, String lesson) {
        if (!mayRead(user, lesson))
            throw new IllegalArgumentException("Không có quyền xem video bài học.");
        return dao.list(lesson);
    }

    public LessonVideo readable(User user, String id) {
        var v = dao.get(id);
        return v != null && mayRead(user, v.lesson()) ? v : null;
    }

    private static String text(String text, int max) {
        if (text == null || text.isBlank() || text.trim().length() > max)
            throw new IllegalArgumentException("Nhập tiêu đề và nguồn video hợp lệ.");
        return text.trim();
    }

    public static String youtubeId(String url) {
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            String id = null;
            if (!"https".equals(uri.getScheme())
                    || host == null
                    || uri.getUserInfo() != null
                    || uri.getPort() != -1) throw new IllegalArgumentException();
            host = host.toLowerCase(Locale.ROOT);
            if (host.equals("youtu.be")) {
                id = uri.getPath().substring(1);
            } else if (Set.of(
                            "www.youtube.com",
                            "youtube.com",
                            "m.youtube.com",
                            "www.youtube-nocookie.com")
                    .contains(host)) {
                String path = uri.getPath();
                if (path.startsWith("/embed/") || path.startsWith("/shorts/"))
                    id = path.substring(path.lastIndexOf('/') + 1);
                else if (path.equals("/watch") && uri.getRawQuery() != null)
                    for (String part : uri.getRawQuery().split("&")) {
                        String[] pair = part.split("=", 2);
                        if (pair.length == 2 && pair[0].equals("v"))
                            id =
                                    URLDecoder.decode(
                                            pair[1], java.nio.charset.StandardCharsets.UTF_8);
                    }
            }
            if (id == null || !id.matches("[A-Za-z0-9_-]{11}"))
                throw new IllegalArgumentException();
            return id;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Dán liên kết HTTPS YouTube hợp lệ của một video, không dùng liên kết danh sách"
                        + " phát.");
        }
    }

    public LessonVideo link(User user, String lesson, String title, String url, String source) {
        requireOwner(user, lesson);
        String id = youtubeId(url);
        String normalized = "https://www.youtube.com/watch?v=" + id;
        if (dao.list(lesson).stream().anyMatch(v -> v.url().equals(normalized)))
            throw new IllegalArgumentException("Video này đã có trong bài học.");
        var v =
                new LessonVideo(
                        UUID.randomUUID().toString(),
                        lesson,
                        user.getEmail(),
                        text(title, 200),
                        "youtube",
                        normalized,
                        text(source, 200),
                        "",
                        "",
                        0,
                        false,
                        Instant.now());
        dao.add(v);
        AuditService.record(user.getEmail(), "Thêm video bài giảng", lesson);
        return v;
    }

    public static Path directory() {
        return Path.of(
                        Database.setting(
                                "SMARTSTU_VIDEO_DIR",
                                Path.of(
                                                System.getProperty("user.home"),
                                                ".smartstu",
                                                "lesson-videos")
                                        .toString()))
                .toAbsolutePath()
                .normalize();
    }

    public static String detect(byte[] header, String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp4")
                && header.length >= 12
                && header[4] == 'f'
                && header[5] == 't'
                && header[6] == 'y'
                && header[7] == 'p') return "video/mp4";
        if (name.endsWith(".webm")
                && header.length >= 4
                && (header[0] & 255) == 0x1a
                && (header[1] & 255) == 0x45
                && (header[2] & 255) == 0xdf
                && (header[3] & 255) == 0xa3) return "video/webm";
        throw new IllegalArgumentException(
                "File không có định dạng MP4/WebM hợp lệ. Hãy xuất video MP4 (H.264/AAC) hoặc"
                    + " WebM.");
    }

    public LessonVideo upload(
            User user, String lesson, String title, String filename, long size, InputStream input)
            throws IOException {
        requireOwner(user, lesson);
        title = text(title, 200);
        if (size <= 0 || size > MAX_BYTES)
            throw new IllegalArgumentException("Video cần có dung lượng từ 1 byte đến 200 MB.");
        if (dao.storedBytes(user.getEmail()) + size > 2L * 1024 * 1024 * 1024)
            throw new IllegalArgumentException(
                    "Kho video giáo viên đã đạt giới hạn 2 GB. Liên hệ quản trị viên hoặc dùng"
                        + " video YouTube.");
        byte[] header = input.readNBytes(16);
        String mime = detect(header, filename), id = UUID.randomUUID().toString();
        Path dir = directory();
        Files.createDirectories(dir);
        String storage = id + (mime.equals("video/mp4") ? ".mp4" : ".webm");
        Path file = dir.resolve(storage);
        try {
            long copied = header.length;
            try (var out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
                out.write(header);
                byte[] block = new byte[65536];
                int count;
                while ((count = input.read(block)) != -1) {
                    copied += count;
                    if (copied > MAX_BYTES || copied > size)
                        throw new IllegalArgumentException("Video vượt giới hạn dung lượng.");
                    out.write(block, 0, count);
                }
            }
            if (copied != size)
                throw new IllegalArgumentException("Video tải lên chưa đầy đủ. Vui lòng thử lại.");
            var v =
                    new LessonVideo(
                            id,
                            lesson,
                            user.getEmail(),
                            title,
                            "upload",
                            "",
                            "Video do giáo viên cung cấp",
                            storage,
                            mime,
                            size,
                            false,
                            Instant.now());
            dao.add(v);
            AuditService.record(user.getEmail(), "Tải video bài giảng", lesson);
            return v;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(file);
            throw e;
        }
    }

    public Path file(LessonVideo video) {
        if (video == null || !video.storage().matches("[a-f0-9-]{36}\\.(mp4|webm)"))
            throw new IllegalArgumentException("Video không hợp lệ.");
        return directory().resolve(video.storage()).normalize();
    }

    public void archive(User user, String id) {
        var v = dao.get(id);
        if (v == null) throw new IllegalArgumentException("Video không còn trong bài học.");
        requireOwner(user, v.lesson());
        dao.archive(id);
        AuditService.record(user.getEmail(), "Ẩn video bài giảng", v.lesson());
    }
}
