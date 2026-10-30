package com.smartstudy.dao;

import com.smartstudy.model.*;
import com.smartstudy.persistence.Database;
import java.sql.*;

public final class EssayPhotoDao {
    private static final String COLUMNS =
            "id,assignment_id,student_email,submission_id,byte_size,width,height,sha256,ocr_text,ocr_warning,ocr_confidence,created_at";

    private EssayPhoto row(ResultSet r) throws SQLException {
        return new EssayPhoto(
                r.getString(1),
                r.getString(2),
                r.getString(3),
                r.getString(4),
                r.getInt(5),
                r.getInt(6),
                r.getInt(7),
                r.getString(8),
                r.getString(9),
                r.getString(10),
                (Double) r.getObject(11),
                r.getTimestamp(12).toInstant());
    }

    private EssayPhoto find(String where, String... args) {
        return Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "SELECT " + COLUMNS + " FROM essay_photos WHERE " + where)) {
                        for (int i = 0; i < args.length; i++) s.setString(i + 1, args[i]);
                        try (var r = s.executeQuery()) {
                            return r.next() ? row(r) : null;
                        }
                    }
                });
    }

    public EssayPhoto get(String id) {
        return find("id=?", id);
    }

    public EssayPhoto draft(String user, String assignment) {
        return find(
                "student_email=? AND assignment_id=? AND submission_id IS NULL", user, assignment);
    }

    public EssayPhoto submitted(String submission) {
        return find("submission_id=?", submission);
    }

    public long used(String user) {
        return Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "SELECT COALESCE(SUM(byte_size),0) FROM essay_photos WHERE"
                                        + " student_email=?")) {
                        s.setString(1, user);
                        try (var r = s.executeQuery()) {
                            r.next();
                            return r.getLong(1);
                        }
                    }
                });
    }

    public void insert(EssayPhoto p, byte[] bytes) {
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "INSERT INTO"
                                        + " essay_photos(id,assignment_id,student_email,image_bytes,byte_size,width,height,sha256)"
                                        + " VALUES(?,?,?,?,?,?,?,?)")) {
                        s.setString(1, p.id());
                        s.setString(2, p.assignment());
                        s.setString(3, p.student());
                        s.setBytes(4, bytes);
                        s.setInt(5, bytes.length);
                        s.setInt(6, p.width());
                        s.setInt(7, p.height());
                        s.setString(8, p.sha256());
                        s.executeUpdate();
                        return null;
                    }
                });
    }

    public byte[] bytes(String id) {
        return Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement("SELECT image_bytes FROM essay_photos WHERE id=?")) {
                        s.setString(1, id);
                        try (var r = s.executeQuery()) {
                            return r.next() ? r.getBytes(1) : null;
                        }
                    }
                });
    }

    public boolean removeDraft(String id, String user) {
        return Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "DELETE FROM essay_photos WHERE id=? AND student_email=? AND"
                                        + " submission_id IS NULL")) {
                        s.setString(1, id);
                        s.setString(2, user);
                        return s.executeUpdate() == 1;
                    }
                });
    }

    public void attach(String user, String assignment, String submission) {
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "UPDATE essay_photos SET submission_id=? WHERE student_email=?"
                                        + " AND assignment_id=? AND submission_id IS NULL")) {
                        s.setString(1, submission);
                        s.setString(2, user);
                        s.setString(3, assignment);
                        s.executeUpdate();
                        return null;
                    }
                });
    }

    public void reading(String id, EssayPhotoReading reading) {
        Database.use(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "UPDATE essay_photos SET"
                                        + " ocr_text=?,ocr_warning=?,ocr_confidence=? WHERE"
                                        + " id=?")) {
                        s.setString(1, reading.text());
                        s.setString(2, reading.warning());
                        s.setDouble(3, reading.confidence());
                        s.setString(4, id);
                        s.executeUpdate();
                        return null;
                    }
                });
    }
}
