package com.smartstudy.web;

import author.*;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;

public final class ViewSupport {
    private ViewSupport() {}

    public static String h(Object value) {
        return value == null
                ? ""
                : String.valueOf(value)
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
    }

    public static String role(User u) {
        return u.getRole().name().toLowerCase(Locale.ROOT);
    }

    public static String roleLabel(User u) {
        return switch (u.getRole()) {
            case ADMIN -> "Quản trị viên";
            case TEACHER -> "Giáo viên";
            case STUDENT -> "Học sinh";
        };
    }

    public static String icon(String key) {
        return switch (key) {
            case "dashboard" -> "home";
            case "courses" -> "courses";
            case "lesson", "resources" -> "book";
            case "quiz", "essays" -> "clipboard";
            case "flashcards" -> "cards";
            case "users", "teachers", "students" -> "users";
            case "ai-questions" -> "sparkles";
            case "ai-tutor" -> "sparkles";
            case "question-bank", "topics" -> "library";
            case "formulas" -> "formula";
            case "mindmap" -> "network";
            case "notifications" -> "bell";
            case "progress", "reports" -> "chart";
            case "profile" -> "user";
            case "audit" -> "history";
            default -> "settings";
        };
    }

    public static String sourceLabel(String source) {
        if (source == null) return "";
        if (source.startsWith("Dữ liệu mẫu")) return "Ngân hàng tham khảo";
        if (source.startsWith("AI hỗ trợ")) return "AI hỗ trợ";
        return source;
    }

    public static String status(String s) {
        return switch (s) {
            case "published" -> "Đã xuất bản";
            case "pending" -> "Chờ duyệt";
            case "rejected" -> "Yêu cầu chỉnh sửa";
            case "hidden" -> "Đã ẩn";
            case "archived" -> "Lưu trữ";
            default -> "Bản nháp";
        };
    }

    public static String date(java.time.Instant i) {
        return i == null
                ? "Chưa có"
                : i.atZone(java.time.ZoneId.of("Asia/Bangkok"))
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public static Map<String, String> menu(User u) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("dashboard", "Tổng quan");
        if (u.getRole() == Role.ADMIN) {
            m.put("users", "Quản lý tài khoản");
            m.put("teachers", "Giám sát giáo viên");
        }
        m.put("courses", u.getRole() == Role.ADMIN ? "Kiểm duyệt khóa học" : "Khóa học");
        m.put(
                "lesson",
                u.getRole() == Role.STUDENT
                        ? "Bài học"
                        : u.getRole() == Role.ADMIN ? "Giám sát học liệu" : "Quản lý bài học");
        m.put("quiz", u.getRole() == Role.ADMIN ? "Giám sát quiz" : "Quiz");
        m.put("flashcards", "Flashcard");
        if (u.getRole() == Role.TEACHER) {
            m.put("resources", "Tài liệu / video");
            m.put("ai-questions", "AI tạo câu hỏi");
            m.put("question-bank", "Ngân hàng câu hỏi");
            m.put("essays", "Bài tự luận");
            m.put("students", "Học sinh");
            m.put("notifications", "Thông báo");
            m.put("ai-settings", "Trạng thái AI");
        }
        if (u.getRole() == Role.STUDENT) {
            m.put("formulas", "Công thức");
            m.put("mindmap", "Sơ đồ tư duy");
            m.put("resources", "Tài liệu");
            m.put("essays", "Bài tự luận");
            m.put("notifications", "Thông báo");
        }
        m.put(u.getRole() == Role.ADMIN ? "reports" : "progress", "Tiến độ & kết quả");
        if (u.getRole() == Role.ADMIN) {
            m.put("topics", "Danh mục và chủ đề");
            m.put("notifications", "Thông báo hệ thống");
            m.put("ai-settings", "Cấu hình AI");
            m.put("audit", "Nhật ký hệ thống");
            m.put("settings", "Cài đặt");
        }
        m.put("ai-tutor", "Trợ lý học tập");
        m.put("profile", "Hồ sơ cá nhân");
        return m;
    }

    public static String current(HttpServletRequest r) {
        Object original = r.getAttribute("jakarta.servlet.forward.path_info");
        String p = original instanceof String ? (String) original : r.getPathInfo();
        return p == null || p.equals("/") ? "dashboard" : p.substring(1);
    }

    public static String csrf(HttpServletRequest r) {
        Object t = r.getSession().getAttribute("csrf");
        if (t == null) {
            t = UUID.randomUUID().toString();
            r.getSession().setAttribute("csrf", t);
        }
        return t.toString();
    }

    public static boolean validCsrf(HttpServletRequest r) {
        return csrf(r).equals(r.getParameter("csrf"));
    }
}
