package com.smartstudy.dao;

import com.smartstudy.model.LessonVideo;
import com.smartstudy.persistence.Database;
import java.sql.*;
import java.util.*;

public final class LessonVideoDao {
    private static LessonVideo read(ResultSet r) throws SQLException {
        return new LessonVideo(
                r.getString("id"),
                r.getString("lesson_id"),
                r.getString("owner_email"),
                r.getString("title"),
                r.getString("source_type"),
                r.getString("source_url"),
                r.getString("source_label"),
                r.getString("storage_name"),
                r.getString("mime_type"),
                r.getLong("byte_size"),
                r.getBoolean("archived"),
                r.getTimestamp("created_at").toInstant());
    }

    public List<LessonVideo> list(String lesson) {
        return Database.use(
                c -> {
                    List<LessonVideo> out = new ArrayList<>();
                    try (var s =
                            c.prepareStatement(
                                    "SELECT * FROM lesson_videos WHERE lesson_id=? AND"
                                        + " archived=FALSE ORDER BY created_at,id")) {
                        s.setString(1, lesson);
                        try (var r = s.executeQuery()) {
                            while (r.next()) out.add(read(r));
                        }
                    }
                    return List.copyOf(out);
                });
    }

    public LessonVideo get(String id) {
        return Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "SELECT * FROM lesson_videos WHERE id=? AND archived=FALSE")) {
                        s.setString(1, id);
                        try (var r = s.executeQuery()) {
                            return r.next() ? read(r) : null;
                        }
                    }
                });
    }

    public void add(LessonVideo v) {
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "INSERT INTO"
                                        + " lesson_videos(id,lesson_id,owner_email,title,source_type,source_url,source_label,storage_name,mime_type,byte_size)"
                                        + " VALUES(?,?,?,?,?,?,?,?,?,?)")) {
                        s.setString(1, v.id());
                        s.setString(2, v.lesson());
                        s.setString(3, v.owner());
                        s.setString(4, v.title());
                        s.setString(5, v.type());
                        s.setString(6, v.url());
                        s.setString(7, v.source());
                        s.setString(8, v.storage());
                        s.setString(9, v.mime());
                        s.setLong(10, v.bytes());
                        s.executeUpdate();
                        return null;
                    }
                });
    }

    public long storedBytes(String owner) {
        return Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "SELECT COALESCE(SUM(byte_size),0) FROM lesson_videos WHERE"
                                        + " owner_email=?")) {
                        s.setString(1, owner);
                        try (var r = s.executeQuery()) {
                            r.next();
                            return r.getLong(1);
                        }
                    }
                });
    }

    public void archive(String id) {
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "UPDATE lesson_videos SET archived=TRUE WHERE id=?")) {
                        s.setString(1, id);
                        s.executeUpdate();
                        return null;
                    }
                });
    }
}
