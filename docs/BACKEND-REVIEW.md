# Hướng dẫn trình bày backend SmartStu

## Backend nằm ở đâu?

Trong NetBeans, mở Source Packages. Trên ổ đĩa, mã Java nằm ở `src/java`.

| Thành phần | Thư mục/file | Trách nhiệm |
|---|---|---|
| Controller | `com/smartstudy/web` | Nhận GET/POST, kiểm tra đầu vào, gọi nghiệp vụ, forward JSP hoặc redirect |
| Service | `com/smartstudy/service` | Chấm quiz, nộp tự luận, phân quyền sở hữu, gọi AI |
| Model | `com/smartstudy/model` | Dữ liệu câu hỏi, quiz, ảnh và kết quả chấm |
| DAO | `com/smartstudy/dao` | Truy vấn JDBC, PreparedStatement, đóng tài nguyên |
| Kết nối/lưu trữ | `com/smartstudy/persistence` | Database, Stores, codec và quản lý transaction |
| Xác thực | `author/AuthFilter.java`, `author/UserStore.java` | Session, vai trò, trạng thái tài khoản, mật khẩu băm |
| View | `web/WEB-INF/views` | JSP hiển thị dữ liệu; CSS/JS ở web/assets |
| Cấu trúc MySQL | `database/smartstu.sql`, `database/migrations` | Bảng, quan hệ và cập nhật schema |

## Luồng dễ trình bày: nộp ảnh tự luận

1. JSP `shared/essays.jsp` và `essay-photo.jspf` hiển thị form.
2. Trình duyệt POST multipart đến `/student/essay-photo`.
3. `AuthFilter` xác minh session/vai trò. `EssayPhotoServlet` kiểm tra CSRF và nhận file.
4. `EssayPhotoService` kiểm tra quyền, định dạng thật, dung lượng, kích thước; chuẩn hóa ảnh và loại metadata.
5. `EssayPhotoDao` lưu JPEG vào MySQL bảng `essay_photos` bằng PreparedStatement.
6. Khi nộp bài, `EssayWorkflow` gắn ảnh vào bài nộp; gửi lại cùng token không tạo kết quả trùng.
7. Giáo viên gọi `AIService` → `GeminiAdapter` để đọc ảnh/chấm theo rubric. Key chỉ ở backend.
8. Điểm AI là đề xuất. Giáo viên sửa/duyệt/công bố; học sinh mới xem được kết quả.

## Các luồng khác để mở code cho thầy xem

- Đăng nhập: `web/AuthServlet.java` → `author/UserStore.java` → `Stores` → `JdbcMap` → MySQL. Mật khẩu dùng PBKDF2WithHmacSHA256, salt riêng, không lưu plaintext. Đăng ký công khai chỉ tạo Student.
- Quiz: `web/QuizController.java` → `service/QuizService.java`. Điểm được tính phía server, không tin điểm do JavaScript gửi lên.
- Hội thoại AI: `web/TutorController.java` → `service/TutorService.java` → `AIService` / `GeminiAdapter`; `dao/ConversationDao.java` lưu hội thoại và kiểm tra chủ sở hữu.
- Transaction: `web/PersistenceFilter.java` và `persistence/Database.java`. Lệnh thay đổi dữ liệu thông thường được commit/rollback; không giữ transaction chung trong lúc chờ API AI.

## Kiến trúc thực tế và giới hạn cần nói đúng

Đây là Java Web MVC dùng Servlet/JSP, JDBC và Service, không phải Spring MVC. Một số JSP còn scriptlet để hiển thị; không nên khẳng định toàn bộ JSP thuần EL/JSTL. Một phần nghiệp vụ dùng `Stores.map` với `JdbcMap` và `RecordCodec` để lưu/đọc record; không phải mỗi entity đều có DAO riêng. Ảnh, video, hội thoại có DAO chuyên biệt. Memory mode chỉ phục vụ kiểm thử độc lập; mặc định ứng dụng dùng MySQL và báo lỗi khi mất kết nối.

Máy hiện tại dùng MySQL riêng tại 127.0.0.1:3307, database smartstu; SSMS không phải công cụ quản lý MySQL. Dùng MySQL Workbench để xem bảng. Cấu hình bí mật nằm ngoài thư mục web; không chiếu nội dung file chứa mật khẩu/API key khi thuyết trình. Xem `DATABASE.md` để cấu hình trên máy khác.

## Kịch bản kiểm tra trực tiếp cùng thầy

1. Đăng nhập Student, mở URL Teacher: phải bị chặn.
2. Làm quiz, xem điểm, tải lại trang: lịch sử vẫn còn trong database.
3. Nộp ảnh bài tự luận; Teacher xem ảnh và đề xuất AI; Student chưa thấy điểm chưa công bố.
4. Teacher công bố kết quả rồi Student tải lại để xem.
5. Trong Workbench chạy SELECT id, assignment_id, submission_id, byte_size, created_at FROM smartstu.essay_photos; để đối chiếu ảnh đã lưu, không cần xuất blob hoặc dữ liệu bí mật.
6. Đóng/mở lại ứng dụng và kiểm tra dữ liệu còn. Không xóa database hoặc chạy lại seed để chứng minh lưu bền vững.

AI có thể nhận dạng sai hoặc hết quota. Cần đối chiếu ảnh gốc và để giáo viên quyết định; không khẳng định AI chính xác tuyệt đối.
