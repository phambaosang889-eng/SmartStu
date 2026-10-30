# SmartStu · Vật lý 10

Java 17 · Jakarta Servlet/JSP · Ant · Tomcat 10.1 · JDBC · MySQL 8.4. Giữ nguyên giao diện HTML/CSS/JavaScript hiện có; không dùng Spring Boot, React hoặc Node.js.

## Chạy dự án

Hướng dẫn đầy đủ ở [docs/DATABASE.md](docs/DATABASE.md): cài MySQL/Workbench, chạy SQL, cấu hình JDBC và connection pool, khởi tạo tài khoản, chạy NetBeans 17 và sao lưu.

Trên máy hiện tại, MySQL riêng của dự án dùng `127.0.0.1:3307`; Tomcat kiểm thử dùng `http://localhost:8081/SmartStudy/home`.

```powershell
pwsh -NoProfile -File tools/start-local-mysql.ps1
pwsh -NoProfile -File tools/start-local-tomcat.ps1
```

Trong NetBeans 17: chọn JDK 17, Tomcat 10.1, Clean and Build rồi Run. Riêng workspace này tự đọc `.runtime/db.local.properties` bên ngoài web khi chạy từ `build/web/WEB-INF/classes`; không cần thêm VM option. Triển khai WAR ở máy khác vẫn phải cấu hình `SMARTSTU_CONFIG_FILE` như DATABASE.md. Chỉ chạy một Tomcat trên cổng 8081; script khởi động kiểm tra cổng và tái sử dụng ứng dụng đang chạy. WAR: `dist/test_smt.war`; triển khai tên `SmartStudy.war` để giữ context path.

Trang đăng nhập có **Tạo tài khoản học sinh** (`/register`): lưu tài khoản MySQL, kiểm tra email trùng, mật khẩu xác nhận và CSRF. Đăng ký công khai luôn có vai trò Student; Teacher/Admin do quản trị viên cấp. Chưa có dịch vụ gửi email xác minh hoặc khôi phục mật khẩu.

## Database

- `database/smartstu.sql`: schema đầy đủ, migration cộng thêm và các view SQL báo cáo.
- `database/migrations/002-integrity.sql`: cập nhật database v1 mà không xóa dữ liệu.
- `database/seed-local.sql`: dữ liệu học tập khởi tạo, tài khoản chỉ có mật khẩu băm, INSERT IGNORE giữ nguyên bản ghi cũ.
- `com.smartstudy.persistence.SeedDatabase`: cách khởi tạo mới với mật khẩu riêng từ cấu hình ngoài source.

Mặc định là MySQL. Khi kết nối lỗi, hệ thống báo lỗi rõ ràng, không chuyển ngầm sang dữ liệu mẫu. Chỉ đặt `SMARTSTU_STORAGE=memory` khi chủ động chạy kiểm thử JVM độc lập; chế độ này không lưu bền vững.

Tài khoản khởi tạo: `student@smartstudy.vn`, `teacher@smartstudy.vn`, `admin@smartstudy.vn`. Mật khẩu là giá trị riêng đã dùng khi khởi tạo, không phải mật khẩu công khai `123456`. Trên máy kiểm thử, xem mục tài khoản trong DATABASE.md. Không đưa file bí mật vào Git/WAR.

## Chức năng và quyền

- Student đăng ký khóa đã duyệt, học bài, lưu tiến độ/thời gian học, ôn flashcard, làm quiz, lưu bản nháp và nộp tự luận, xem kết quả đã công bố.
- Teacher quản lý khóa/chương/học liệu, học sinh và bài nộp thuộc phạm vi phụ trách; quản lý ngân hàng, quiz và rubric; rà soát câu hỏi/đề xuất chấm AI trước khi sử dụng.
- Admin quản lý tài khoản, khóa/mở khóa, vai trò, danh mục, kiểm duyệt khóa học, báo cáo, audit và chính sách AI. Không chỉnh điểm chuyên môn hoặc đọc hội thoại riêng trái quyền.
- Dashboard và CSV dùng dữ liệu học tập đã lưu. Quiz tính điểm trên server, có thời hạn, giới hạn số lần và chống nộp trùng. Điểm tự luận chỉ hiển thị sau khi giáo viên công bố.
- Backend kiểm tra session, vai trò và quyền sở hữu; POST có CSRF, validation và transaction. Form có phiên bản để phát hiện sửa từ tab cũ. Login/logout có CSRF; mật khẩu PBKDF2 với salt ngẫu nhiên. Dữ liệu người dùng được escape khi hiển thị.

## AI Tutor và AI giáo viên

Menu **Trợ lý học tập** có hội thoại cá nhân, lịch sử MySQL, Enter gửi/Shift+Enter xuống dòng, trạng thái xử lý/lỗi và gửi lại không tạo tin trùng. Hỗ trợ qua gateway: giải thích, hướng dẫn từng bước, tóm tắt kiến thức.

Trang giáo viên vẫn có quy trình chuẩn bị câu hỏi → chỉnh sửa/chọn lọc → lưu ngân hàng → tạo quiz; tự luận có đề, rubric, chấm thủ công, đề xuất AI, rà soát và công bố.

Gemini đã được tích hợp trực tiếp từ backend Java, dùng key trong cấu hình private và đã kiểm thử thật 3/3 tác vụ hỏi đáp, tạo câu hỏi và chấm rubric. Xem [docs/GEMINI.md](docs/GEMINI.md). Gateway tùy chỉnh cũ vẫn được hỗ trợ theo [docs/AI-GATEWAY.md](docs/AI-GATEWAY.md). Khi dịch vụ lỗi, không trả lời AI giả hoặc chấm điểm ngẫu nhiên.

## Video bài giảng

Bài học hỗ trợ video YouTube có ghi nguồn và video MP4/WebM giáo viên tải lên, phát ngay trong bài học. Teacher → Bài học → Chỉnh sửa → Video bài giảng. Chạy migration `database/migrations/003-lesson-videos.sql` trên database cũ; xem [docs/LESSON-VIDEOS.md](docs/LESSON-VIDEOS.md) cho lưu trữ, nguồn và giới hạn.

## Build và kiểm thử

Giao diện điện thoại tự áp dụng theo chiều rộng màn hình; giữ nguyên URL và dữ liệu như desktop. Xem [báo cáo responsive và dọn file](docs/MOBILE-CLEANUP-20261028.md).

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.20'
& 'C:/Program Files/NetBeans-17/netbeans/extide/ant/bin/ant.bat' dist
pwsh -NoProfile -File tools/run-db-regression.ps1
```

- `test/DomainWorkflowTest.java`: nghiệp vụ độc lập trong chế độ memory.
- `test/AuthRoleTest.java`: xác thực đúng/sai và vai trò, chạy được với MySQL hoặc memory.
- `test/RelationalWorkflowTest.java`: khóa học/chương/học liệu, câu hỏi, quiz, tự luận/rubric, quyền sở hữu và UTC qua JDBC; rollback dữ liệu kiểm thử.
- `test/DatabaseIntegrationTest.java`: CRUD, rollback, hội thoại, retry và mốc xác minh sau restart.
- `tools/http-regression.ps1`: kiểm tra ba vai trò qua HTTP, menu, encoding, CSRF, quiz/tự luận, AI Tutor, XSS, đăng xuất, khóa tài khoản và CSV. Chỉ chạy trên môi trường fixture cục bộ, vì kiểm thử có thay đổi dữ liệu và tạm khóa tài khoản Minh.
- `tools/check-schema.ps1`: thử schema/seed hai lần trên database tên riêng, không đụng database hiện có.

Source chính ở `src/java` và `web`. `.runtime` chứa MySQL kiểm thử, dữ liệu và cấu hình riêng, không đóng gói vào WAR. Bản sao lưu trước khi tích hợp database: `backups/smartstu-before-database-20261026.zip`. Kiến trúc và giới hạn lưu trữ được mô tả trong [docs/MVC.md](docs/MVC.md) và DATABASE.md.

## Thư mục cần làm việc

| Thư mục | Nội dung |
|---|---|
| `src/java`, `web` | Java MVC/DAO/service, JSP và tài nguyên giao diện |
| `database` | Schema, migration và học liệu khởi tạo |
| `test`, `tools` | Source kiểm thử và công cụ build/vận hành đang dùng |
| `docs` | Hướng dẫn, giới hạn và bằng chứng kiểm thử |
| `nbproject`, `build.xml` | Cấu hình NetBeans/Ant |
| `build`, `dist` | Bản ứng dụng Tomcat đang dùng và WAR |
| `.runtime`, `backups` | Dữ liệu/cấu hình riêng, cache kiểm thử có thể tái tạo và bản khôi phục; không đóng gói vào WAR |

Biên dịch lại test bằng `tools/compile-checks.ps1` sau khi build. Classpath kiểm thử: `build/web/WEB-INF/classes;.runtime/test-classes;web/WEB-INF/lib/*`. Script HTTP dùng PowerShell 7; build và Run NetBeans vẫn dùng Java, không phụ thuộc pwsh. Không xóa `.runtime` để dọn cache vì thư mục này còn chứa database và video thật.
