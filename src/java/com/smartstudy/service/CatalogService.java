package com.smartstudy.service;

import author.Role;
import author.User;
import com.smartstudy.model.CatalogEntry;
import java.time.Instant;
import java.util.*;

/** Shared content service; durable repositories are selected by the storage configuration. */
public final class CatalogService {
    private static final CatalogService INSTANCE = new CatalogService();
    private final Map<String, CatalogEntry> entries =
            com.smartstudy.persistence.Stores.map("catalog_entries", CatalogEntry.class);
    public static final Map<String, String> LABELS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("courses", "Khóa học");
        m.put("lesson", "Bài học");
        m.put("flashcards", "Flashcard");
        m.put("students", "Học sinh");
        m.put("teachers", "Giáo viên");
        m.put("notifications", "Thông báo");
        m.put("resources", "Tài liệu / video");
        m.put("topics", "Danh mục và chủ đề");
        LABELS = Collections.unmodifiableMap(m);
    }

    public static CatalogService getInstance() {
        return INSTANCE;
    }

    private CatalogService() {
        if (!com.smartstudy.persistence.Stores.seed() || !entries.isEmpty()) return;
        seed(
                "topic-physics",
                "topics",
                "Vật lý 10",
                "Môn Vật lý lớp 10: chuyển động, lực và năng lượng.",
                "Môn học");
        seed(
                "topic-motion",
                "topics",
                "Chuyển động",
                "Mô tả và phân tích chuyển động của vật.",
                "Chủ đề");
        seed(
                "topic-force",
                "topics",
                "Động lực học",
                "Các định luật Newton và lực trong thực tế.",
                "Chủ đề");
        seed(
                "topic-energy",
                "topics",
                "Năng lượng",
                "Công, công suất và các dạng năng lượng.",
                "Chủ đề");
        seed(
                "course-mechanics",
                "courses",
                "Vật lý 10 · Cơ học",
                "Lộ trình học từ chuyển động đến lực, công và năng lượng. Đọc bài học, luyện"
                    + " flashcard, làm quiz và trình bày bài tự luận để củng cố kiến thức.",
                "Vật lý 10");
        seed(
                "lesson-motion",
                "lesson",
                "Chuyển động thẳng biến đổi đều",
                "MỤC TIÊU\n"
                    + "Phân biệt vận tốc và gia tốc; vận dụng phương trình vận tốc khi gia tốc"
                    + " không đổi.\n\n"
                    + "LÝ THUYẾT\n"
                    + "Vận tốc cho biết mức độ nhanh chậm và hướng chuyển động. Gia tốc a = (v −"
                    + " v₀)/t mô tả sự thay đổi vận tốc. Trong chuyển động thẳng biến đổi đều, gia"
                    + " tốc không đổi. Chọn chiều dương trước khi gán dấu.\n\n"
                    + "CÔNG THỨC\n"
                    + "v = v₀ + at; s = v₀t + ½at². v có đơn vị m/s, a có đơn vị m/s², t tính bằng"
                    + " giây.\n\n"
                    + "VÍ DỤ\n"
                    + "Xe có v₀ = 2 m/s, a = 3 m/s² trong 2 s: v = 2 + 3×2 = 8 m/s; s = 2×2 +"
                    + " ½×3×2² = 10 m.\n\n"
                    + "TỰ KIỂM TRA\n"
                    + "Một vật đang đi theo chiều dương và chậm dần: gia tốc mang dấu nào?\n"
                    + "Đáp án: âm, vì gia tốc ngược hướng vận tốc.",
                "Chuyển động");
        seed(
                "lesson-newton",
                "lesson",
                "Ba định luật Newton",
                "MỤC TIÊU\n"
                    + "Xác định hợp lực và giải thích mối liên hệ giữa lực, khối lượng, gia"
                    + " tốc.\n\n"
                    + "ĐỊNH LUẬT I\n"
                    + "Khi hợp lực bằng không, vật đứng yên hoặc chuyển động thẳng đều. Quán tính"
                    + " là tính chất giữ nguyên trạng thái chuyển động.\n\n"
                    + "ĐỊNH LUẬT II\n"
                    + "Vectơ F = m vectơ a. Gia tốc cùng hướng hợp lực. F tính bằng N, m bằng kg và"
                    + " a bằng m/s². Chỉ sử dụng lực tổng hợp tác dụng lên cùng một vật.\n\n"
                    + "ĐỊNH LUẬT III\n"
                    + "Hai vật tương tác bằng cặp lực cùng độ lớn, ngược chiều, đặt lên hai vật"
                    + " khác nhau nên không triệt tiêu nhau khi xét từng vật.\n\n"
                    + "VÍ DỤ\n"
                    + "Vật 2 kg chịu hợp lực 10 N: a = F/m = 10/2 = 5 m/s², cùng hướng lực.\n\n"
                    + "LUYỆN TẬP\n"
                    + "Vật 4 kg có gia tốc 3 m/s². Hợp lực bằng bao nhiêu? Đáp án: 12 N.\n\n"
                    + "LỖI THƯỜNG GẶP\n"
                    + "Nhầm lực thành phần với hợp lực; viết gia tốc cùng hướng vận tốc trong mọi"
                    + " trường hợp.",
                "Động lực học");
        seed(
                "lesson-friction",
                "lesson",
                "Lực ma sát trượt",
                "MỤC TIÊU\n"
                    + "Nhận diện lực ma sát và sử dụng mô hình Fms = μN.\n\n"
                    + "LÝ THUYẾT\n"
                    + "Lực ma sát trượt ngược chiều chuyển động tương đối của hai bề mặt. N là độ"
                    + " lớn phản lực pháp tuyến; chỉ khi không có gia tốc theo phương thẳng đứng và"
                    + " không có lực đứng khác mới có N = mg.\n\n"
                    + "VÍ DỤ\n"
                    + "Vật 2 kg trượt trên mặt phẳng ngang, μ = 0,2; g = 10 m/s². N = mg = 20 N."
                    + " Fms = 0,2×20 = 4 N.\n\n"
                    + "LUYỆN TẬP\n"
                    + "Nếu N tăng gấp đôi và μ không đổi, Fms tăng gấp đôi.",
                "Động lực học");
        seed(
                "lesson-energy",
                "lesson",
                "Công và năng lượng",
                "MỤC TIÊU\n"
                    + "Tính công của lực không đổi và động năng.\n\n"
                    + "LÝ THUYẾT\n"
                    + "Công W = Fs cos α, α là góc giữa lực và độ dời; đơn vị joule (J). Công suất"
                    + " P = W/t, đơn vị watt (W). Động năng Wđ = ½mv². Khi chỉ có trọng lực và lực"
                    + " đàn hồi sinh công, cơ năng được bảo toàn.\n\n"
                    + "VÍ DỤ\n"
                    + "Lực 10 N cùng hướng độ dời 3 m: W = 30 J. Thực hiện trong 2 s: P = 15 W. Vật"
                    + " 2 kg chạy với v = 3 m/s có động năng 9 J.\n\n"
                    + "LUYỆN TẬP\n"
                    + "Lực vuông góc độ dời có công bằng 0 vì cos 90° = 0.",
                "Năng lượng");
        seed(
                "card-newton",
                "flashcards",
                "Định luật II Newton có công thức gì?",
                "F = ma. Gia tốc cùng hướng hợp lực; đơn vị m/s².",
                "Động lực học");
        seed(
                "card-speed",
                "flashcards",
                "Tính gia tốc trung bình như thế nào?",
                "a = Δv/Δt. Cần xác định dấu của vận tốc và sử dụng cùng hệ đơn vị.",
                "Chuyển động");
        seed(
                "card-work",
                "flashcards",
                "Công của lực không đổi được tính ra sao?",
                "W = Fs cos α. Công dương khi lực có thành phần cùng hướng độ dời.",
                "Năng lượng");
        seed(
                "resource-guide",
                "resources",
                "Phiếu hướng dẫn giải bài tập lực",
                "1. Vẽ vật và các lực tác dụng lên vật.\n"
                    + "2. Chọn hệ trục và chiều dương.\n"
                    + "3. Viết phương trình tổng hợp lực theo từng trục.\n"
                    + "4. Áp dụng ΣF = ma, thay số trong hệ SI.\n"
                    + "5. Kiểm tra dấu, đơn vị và tính hợp lý của kết quả.\n\n"
                    + "Bài tự luyện: Vật 2 kg trên mặt phẳng ngang không ma sát chịu lực kéo 8 N."
                    + " Tính gia tốc.\n"
                    + "Đối chiếu: a = 8/2 = 4 m/s², cùng hướng lực.\n\n"
                    + "Nguồn: Học liệu demo do SmartStu biên soạn. Giáo viên có thể đính kèm đường"
                    + " dẫn tài liệu hoặc video HTTPS ở phần chỉnh sửa.",
                "Động lực học");
        seed(
                "student-minh",
                "students",
                "Minh",
                "Học sinh demo · student@smartstudy.vn. Tài khoản demo đăng nhập được cung cấp ở"
                    + " trang đăng nhập.",
                "10A1");
        seed(
                "teacher-huong",
                "teachers",
                "Cô Hương",
                "Giáo viên demo · teacher@smartstudy.vn · phụ trách Vật lý 10.",
                "10A1");
        seed(
                "notice-review",
                "notifications",
                "Ôn tập định luật Newton",
                "Đọc bài Ba định luật Newton, ôn flashcard và hoàn thành bài luyện tập. Khi viết tự"
                    + " luận, nhớ trình bày công thức, thay số và đơn vị.",
                "10A1");
    }

    private void seed(String id, String kind, String title, String body, String topic) {
        entries.put(
                id,
                new CatalogEntry(
                        id,
                        kind,
                        title,
                        body,
                        topic,
                        "published",
                        "teacher@smartstudy.vn",
                        Instant.now(),
                        1));
    }

    public synchronized List<CatalogEntry> list(String kind, User actor) {
        return entries.values().stream()
                .filter(e -> e.kind().equals(kind))
                .filter(
                        e ->
                                actor.getRole() == Role.ADMIN
                                        || e.owner().equals(actor.getEmail())
                                        || actor.getRole() == Role.STUDENT
                                                && e.status().equals("published"))
                .toList();
    }

    public synchronized List<CatalogEntry> published(String kind) {
        return entries.values().stream()
                .filter(e -> e.kind().equals(kind) && e.status().equals("published"))
                .toList();
    }

    public synchronized CatalogEntry get(String id) {
        return entries.get(id);
    }

    public static void authorize(String kind, User actor) {
        if (kind == null
                || !(actor.getRole() == Role.TEACHER
                                && Set.of(
                                                "courses",
                                                "lesson",
                                                "flashcards",
                                                "resources",
                                                "notifications")
                                        .contains(kind)
                        || actor.getRole() == Role.ADMIN
                                && Set.of("topics", "notifications").contains(kind)))
            throw new IllegalArgumentException("Bạn không có quyền quản lý mục này.");
    }

    public synchronized CatalogEntry save(
            User actor,
            String kind,
            String id,
            String title,
            String body,
            String topic,
            String status,
            int version) {
        authorize(kind, actor);
        title = required(title, 150, "Tiêu đề");
        body = required(body, 12000, "Nội dung");
        topic = required(topic, 80, "Chủ đề / lớp");
        if (status == null
                || !(kind.equals("courses")
                                ? Set.of("draft", "pending", "archived")
                                : Set.of("draft", "published", "archived"))
                        .contains(status))
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ. Khóa học phải gửi quản trị viên duyệt.");
        CatalogEntry old = id == null || id.isBlank() ? null : entries.get(id);
        if (id != null && !id.isBlank() && old == null)
            throw new IllegalArgumentException("Không tìm thấy bản ghi.");
        if (old != null
                && (!old.kind().equals(kind)
                        || actor.getRole() != Role.ADMIN && !old.owner().equals(actor.getEmail())))
            throw new IllegalArgumentException("Không có quyền sửa bản ghi.");
        if (old != null && version != old.version())
            throw new IllegalArgumentException(
                    "Nội dung đã thay đổi ở tab khác. Tải lại trước khi lưu.");
        if (kind.equals("topics") && !status.equals("archived")) {
            String normalizedTitle = topicKey(title), normalizedGroup = topicKey(topic);
            for (CatalogEntry existing : entries.values())
                if (existing.kind().equals("topics")
                        && !existing.status().equals("archived")
                        && (old == null || !existing.id().equals(old.id()))
                        && topicKey(existing.title()).equals(normalizedTitle)
                        && topicKey(existing.topic()).equals(normalizedGroup))
                    throw new IllegalArgumentException(
                            "Danh mục / chủ đề này đã tồn tại. Hãy chỉnh sửa mục hiện có thay vì"
                                + " thêm bản trùng.");
        }
        String key = old == null ? UUID.randomUUID().toString() : old.id();
        CatalogEntry next =
                new CatalogEntry(
                        key,
                        kind,
                        title,
                        body,
                        topic,
                        status,
                        old == null ? actor.getEmail() : old.owner(),
                        Instant.now(),
                        old == null ? 1 : old.version() + 1,
                        old == null ? Instant.now() : old.createdAt());
        entries.put(key, next);
        return next;
    }

    public synchronized void moderate(
            User admin, String id, String status, String reason, int version) {
        if (admin.getRole() != Role.ADMIN
                || status == null
                || !Set.of("published", "rejected", "hidden", "archived").contains(status))
            throw new IllegalArgumentException("Không có quyền kiểm duyệt.");
        CatalogEntry e = entries.get(id);
        if (e == null || !e.kind().equals("courses"))
            throw new IllegalArgumentException("Không tìm thấy khóa học.");
        if (e.version() != version)
            throw new IllegalArgumentException("Khóa học đã thay đổi. Tải lại trước khi duyệt.");
        reason = required(reason, 1000, "Lý do kiểm duyệt");
        entries.put(
                id,
                new CatalogEntry(
                        e.id(),
                        e.kind(),
                        e.title(),
                        e.body(),
                        e.topic(),
                        status,
                        e.owner(),
                        Instant.now(),
                        e.version() + 1,
                        e.createdAt()));
        AuditService.record(
                admin.getEmail(), "Kiểm duyệt khóa học: " + status, id + " · " + reason);
    }

    public static String required(String text, int max, String field) {
        if (text == null || text.trim().isEmpty() || text.length() > max)
            throw new IllegalArgumentException(field + " phải có từ 1 đến " + max + " ký tự.");
        return text.trim();
    }

    private static String topicKey(String text) {
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)
                .strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
