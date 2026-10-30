package com.smartstudy.dao;

import author.User;
import com.smartstudy.persistence.Database;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class ConversationDao {
    public record Conversation(String id, String title, Instant created) {}

    public record Message(
            long id,
            String requestId,
            String role,
            String content,
            String status,
            Instant created) {}

    public List<Conversation> list(User user) {
        return list(user, 1).stream().limit(50).toList();
    }

    public List<Conversation> list(User user, int page) {
        return Database.use(
                c -> {
                    List<Conversation> rows = new ArrayList<>();
                    try (var s =
                            c.prepareStatement(
                                    "SELECT id,title,created_at FROM ai_conversations WHERE"
                                        + " owner_email=? AND deleted_at IS NULL ORDER BY"
                                        + " created_at DESC,id DESC LIMIT 51 OFFSET ?")) {
                        s.setString(1, user.getEmail());
                        s.setInt(2, (Math.max(1, Math.min(page, 1000)) - 1) * 50);
                        try (var r = s.executeQuery()) {
                            while (r.next())
                                rows.add(
                                        new Conversation(
                                                r.getString(1),
                                                r.getString(2),
                                                r.getTimestamp(3).toInstant()));
                        }
                    }
                    return rows;
                });
    }

    public String create(User user) {
        String id = UUID.randomUUID().toString();
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "INSERT INTO ai_conversations(id,owner_email,title)"
                                        + " VALUES(?,?,?)")) {
                        s.setString(1, id);
                        s.setString(2, user.getEmail());
                        s.setString(3, "Hội thoại mới");
                        s.executeUpdate();
                        return null;
                    }
                });
        return id;
    }

    private void requireOwner(Connection c, User user, String id, boolean lock)
            throws SQLException {
        try (var s =
                c.prepareStatement(
                        "SELECT id FROM ai_conversations WHERE id=? AND owner_email=? AND"
                            + " deleted_at IS NULL"
                                + (lock ? " FOR UPDATE" : ""))) {
            s.setString(1, id);
            s.setString(2, user.getEmail());
            try (var r = s.executeQuery()) {
                if (!r.next())
                    throw new IllegalArgumentException("Không tìm thấy hội thoại của bạn.");
            }
        }
    }

    public List<Message> messages(User user, String id) {
        return Database.use(
                c -> {
                    requireOwner(c, user, id, false);
                    List<Message> rows = new ArrayList<>();
                    try (var s =
                            c.prepareStatement(
                                    "SELECT id,request_id,role,content,status,created_at FROM"
                                        + " ai_messages WHERE conversation_id=? ORDER BY id LIMIT"
                                        + " 100")) {
                        s.setString(1, id);
                        try (var r = s.executeQuery()) {
                            while (r.next())
                                rows.add(
                                        new Message(
                                                r.getLong(1),
                                                r.getString(2),
                                                r.getString(3),
                                                r.getString(4),
                                                r.getString(5),
                                                r.getTimestamp(6).toInstant()));
                        }
                    }
                    return rows;
                });
    }

    /**
     * Short transaction claims one request. Provider calls happen after releasing the connection.
     */
    public boolean claim(User user, String id, String request, String text) {
        return atomic(
                () ->
                        Database.use(
                                c -> {
                                    requireOwner(c, user, id, true);
                                    try (var s =
                                            c.prepareStatement(
                                                    "SELECT role,content,status,created_at FROM"
                                                        + " ai_messages WHERE conversation_id=? AND"
                                                        + " request_id=? ORDER BY id")) {
                                        s.setString(1, id);
                                        s.setString(2, request);
                                        try (var r = s.executeQuery()) {
                                            while (r.next()) {
                                                if ("user".equals(r.getString(1))
                                                        && !text.equals(r.getString(2)))
                                                    throw new IllegalArgumentException(
                                                            "Mã gửi lại không khớp nội dung ban"
                                                                + " đầu.");
                                                if ("assistant".equals(r.getString(1))) {
                                                    if ("complete".equals(r.getString(3))
                                                            || "pending".equals(r.getString(3))
                                                                    && r.getTimestamp(4)
                                                                            .toInstant()
                                                                            .isAfter(
                                                                                    Instant.now()
                                                                                            .minusSeconds(
                                                                                                    120)))
                                                        return false;
                                                    try (var retry =
                                                            c.prepareStatement(
                                                                    "UPDATE ai_messages SET"
                                                                        + " status='pending',content='',created_at=CURRENT_TIMESTAMP(6)"
                                                                        + " WHERE conversation_id=?"
                                                                        + " AND request_id=? AND"
                                                                        + " role='assistant'")) {
                                                        retry.setString(1, id);
                                                        retry.setString(2, request);
                                                        retry.executeUpdate();
                                                        return true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    try (var s =
                                            c.prepareStatement(
                                                    "SELECT COUNT(*) FROM ai_messages WHERE"
                                                        + " conversation_id=? AND role='assistant'"
                                                        + " AND status='pending' AND"
                                                        + " created_at>UTC_TIMESTAMP()-INTERVAL 2"
                                                        + " MINUTE")) {
                                        s.setString(1, id);
                                        try (var r = s.executeQuery()) {
                                            r.next();
                                            if (r.getInt(1) > 0)
                                                throw new IllegalArgumentException(
                                                        "Hội thoại đang xử lý câu hỏi trước. Vui"
                                                            + " lòng chờ kết quả hoặc tải lại"
                                                            + " trang.");
                                        }
                                    }
                                    try (var s =
                                            c.prepareStatement(
                                                    "SELECT COUNT(*) FROM ai_messages WHERE"
                                                        + " conversation_id=?")) {
                                        s.setString(1, id);
                                        try (var r = s.executeQuery()) {
                                            r.next();
                                            if (r.getInt(1) >= 100)
                                                throw new IllegalArgumentException(
                                                        "Hội thoại đã đủ 50 lượt. Hãy tạo hội thoại"
                                                            + " mới.");
                                        }
                                    }
                                    try (var s =
                                            c.prepareStatement(
                                                    "INSERT INTO"
                                                        + " ai_messages(conversation_id,request_id,role,content,status)"
                                                        + " VALUES(?,?,'user',?,'complete'),(?,?,'assistant','','pending')")) {
                                        s.setString(1, id);
                                        s.setString(2, request);
                                        s.setString(3, text);
                                        s.setString(4, id);
                                        s.setString(5, request);
                                        s.executeUpdate();
                                    }
                                    try (var s =
                                            c.prepareStatement(
                                                    "UPDATE ai_conversations SET title=? WHERE id=?"
                                                        + " AND title='Hội thoại mới'")) {
                                        s.setString(
                                                1, text.substring(0, Math.min(70, text.length())));
                                        s.setString(2, id);
                                        s.executeUpdate();
                                    }
                                    return true;
                                }));
    }

    public void finish(User user, String id, String request, String answer, boolean success) {
        atomic(
                () ->
                        Database.use(
                                c -> {
                                    requireOwner(c, user, id, true);
                                    try (var s =
                                            c.prepareStatement(
                                                    "UPDATE ai_messages SET content=?,status=?"
                                                        + " WHERE conversation_id=? AND"
                                                        + " request_id=? AND role='assistant' AND"
                                                        + " status='pending'")) {
                                        s.setString(1, answer);
                                        s.setString(2, success ? "complete" : "failed");
                                        s.setString(3, id);
                                        s.setString(4, request);
                                        s.executeUpdate();
                                        return null;
                                    }
                                }));
    }

    public void hide(User user, String id) {
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "UPDATE ai_conversations SET deleted_at=CURRENT_TIMESTAMP(6)"
                                        + " WHERE id=? AND owner_email=? AND deleted_at IS NULL")) {
                        s.setString(1, id);
                        s.setString(2, user.getEmail());
                        if (s.executeUpdate() != 1)
                            throw new IllegalArgumentException("Không tìm thấy hội thoại của bạn.");
                        return null;
                    }
                });
    }

    private static <T> T atomic(java.util.function.Supplier<T> work) {
        try {
            Database.begin();
            try {
                T value = work.get();
                Database.end(true);
                return value;
            } catch (RuntimeException | Error e) {
                Database.end(false);
                throw e;
            }
        } catch (SQLException e) {
            throw new Database.StorageException(e);
        }
    }
}
