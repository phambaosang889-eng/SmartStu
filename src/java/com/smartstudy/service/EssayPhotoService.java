package com.smartstudy.service;

import author.*;
import com.smartstudy.dao.EssayPhotoDao;
import com.smartstudy.model.*;
import com.smartstudy.persistence.Database;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;

public final class EssayPhotoService {
    public static final int MAX_UPLOAD = 8 * 1024 * 1024, MAX_STORED = 2 * 1024 * 1024;
    private final EssayPhotoDao dao = new EssayPhotoDao();

    public record PreparedImage(byte[] bytes, int width, int height, String hash) {}

    public EssayPhoto draft(User user, String assignment) {
        return Database.memory() ? null : dao.draft(user.getEmail(), assignment);
    }

    public EssayPhoto submitted(User user, String submission) {
        return Database.memory() || EssayWorkflow.getInstance().submission(submission, user) == null
                ? null
                : dao.submitted(submission);
    }

    public EssayPhoto readable(User user, String id) {
        if (Database.memory() || user == null || id == null) return null;
        EssayPhoto p = dao.get(id);
        if (p == null) return null;
        if (p.submission() != null)
            return (user.getRole() == Role.STUDENT || user.getRole() == Role.TEACHER)
                            && EssayWorkflow.getInstance().submission(p.submission(), user) != null
                    ? p
                    : null;
        return user.getRole() == Role.STUDENT
                        && p.student().equals(user.getEmail())
                        && EssayWorkflow.getInstance().assignment(p.assignment(), user) != null
                ? p
                : null;
    }

    private void editable(User u, String assignment) {
        if (Database.memory())
            throw new IllegalArgumentException("Ảnh bài làm cần cơ sở dữ liệu MySQL.");
        var a = EssayWorkflow.getInstance().assignment(assignment, u);
        if (u.getRole() != Role.STUDENT || a == null)
            throw new IllegalArgumentException("Bạn không có quyền nộp ảnh cho bài này.");
        if (a.deadline() != null && LocalDate.now().isAfter(a.deadline()))
            throw new IllegalArgumentException("Bài đã hết hạn nộp.");
    }

    public EssayPhoto upload(User u, String assignment, InputStream input) throws IOException {
        editable(u, assignment);
        PreparedImage image = prepare(input);
        EssayPhoto existing = draft(u, assignment);
        if (existing != null) {
            if (existing.sha256().equals(image.hash())) return existing;
            throw new IllegalArgumentException(
                    "Bài nháp đã có ảnh. Gỡ ảnh hiện tại trước khi chọn ảnh khác.");
        }
        if (dao.used(u.getEmail()) + image.bytes().length > 50L * 1024 * 1024)
            throw new IllegalArgumentException(
                    "Đã đạt giới hạn lưu ảnh 50 MB. Gỡ các ảnh nháp không dùng hoặc liên hệ giáo"
                        + " viên.");
        var photo =
                new EssayPhoto(
                        UUID.randomUUID().toString(),
                        assignment,
                        u.getEmail(),
                        null,
                        image.bytes().length,
                        image.width(),
                        image.height(),
                        image.hash(),
                        null,
                        null,
                        null,
                        Instant.now());
        dao.insert(photo, image.bytes());
        return photo;
    }

    public void remove(User u, String id) {
        EssayPhoto p = readable(u, id);
        if (p == null || p.submission() != null)
            throw new IllegalArgumentException("Chỉ có thể gỡ ảnh nháp của bạn.");
        editable(u, p.assignment());
        if (!dao.removeDraft(id, u.getEmail()))
            throw new IllegalArgumentException("Ảnh đã được nộp hoặc thay đổi. Hãy tải lại trang.");
    }

    public byte[] bytes(User u, String id) {
        if (readable(u, id) == null) throw new IllegalArgumentException("Không có quyền xem ảnh.");
        return dao.bytes(id);
    }

    public EssayPhoto scan(User u, String id) {
        EssayPhoto p = readable(u, id);
        if (p == null) throw new IllegalArgumentException("Không có quyền đọc ảnh này.");
        if (u.getRole() == Role.STUDENT && p.submission() != null)
            throw new IllegalArgumentException("Ảnh đã nộp chỉ được giáo viên xử lý.");
        if (p.transcription() != null)
            return p; // Repeated clicks reuse the saved scan, no extra API cost.
        var reading = AIService.getInstance().readEssayPhoto(u, dao.bytes(id));
        dao.reading(id, reading);
        return dao.get(id);
    }

    public void attach(User u, String assignment, String submission) {
        if (!Database.memory()) dao.attach(u.getEmail(), assignment, submission);
    }

    public static PreparedImage prepare(InputStream input) throws IOException {
        byte[] source = input.readNBytes(MAX_UPLOAD + 1);
        if (source.length == 0 || source.length > MAX_UPLOAD)
            throw new IllegalArgumentException("Ảnh phải có dung lượng từ 1 byte đến 8 MB.");
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(source))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext())
                throw new IllegalArgumentException("File không phải ảnh JPEG hoặc PNG hợp lệ.");
            var reader = readers.next();
            BufferedImage image;
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!Set.of("jpeg", "jpg", "png").contains(format))
                    throw new IllegalArgumentException(
                            "Chỉ nhận ảnh JPEG và PNG. Hãy xuất ảnh HEIC sang JPEG trước.");
                reader.setInput(stream, true, true);
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 100 || h < 100 || (long) w * h > 25000000)
                    throw new IllegalArgumentException(
                            "Ảnh cần ít nhất 100 × 100 pixel và không quá 25 megapixel.");
                image = reader.read(0);
            } finally {
                reader.dispose();
            }
            double scale = Math.min(1, 2000.0 / Math.max(image.getWidth(), image.getHeight()));
            int w = Math.max(1, (int) Math.round(image.getWidth() * scale)),
                    h = Math.max(1, (int) Math.round(image.getHeight() * scale));
            BufferedImage clean = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = clean.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, w, h);
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.drawImage(image, 0, 0, w, h, null);
            } finally {
                graphics.dispose();
                image.flush();
            }
            byte[] jpeg;
            try (var out = new ByteArrayOutputStream();
                    var imageOut = new MemoryCacheImageOutputStream(out)) {
                var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                try {
                    var settings = writer.getDefaultWriteParam();
                    settings.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    settings.setCompressionQuality(.88f);
                    writer.setOutput(imageOut);
                    writer.write(null, new IIOImage(clean, null, null), settings);
                    imageOut.flush();
                    jpeg = out.toByteArray();
                } finally {
                    writer.dispose();
                    clean.flush();
                }
            }
            if (jpeg.length > MAX_STORED)
                throw new IllegalArgumentException(
                        "Ảnh quá nhiều chi tiết. Cắt gọn vùng bài làm rồi tải lại.");
            try {
                return new PreparedImage(
                        jpeg,
                        w,
                        h,
                        HexFormat.of()
                                .formatHex(MessageDigest.getInstance("SHA-256").digest(jpeg)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        } catch (javax.imageio.IIOException e) {
            throw new IllegalArgumentException(
                    "Không đọc được ảnh. Hãy chụp lại hoặc chọn JPEG/PNG khác.");
        }
    }
}
