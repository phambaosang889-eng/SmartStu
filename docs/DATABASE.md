# MySQL, JDBC và cách chạy SmartStu

## Môi trường

### Chạy lại sau khi mở máy (cập nhật 27/10/2026)

MySQL ZIP trong `.runtime` là tiến trình riêng, không phải dịch vụ Windows tự chạy khi mở máy. Nếu chỉ khởi động Tomcat mà MySQL chưa bật, trang có dữ liệu sẽ trả 503. `build.xml` nay có hook `-pre-run-deploy` cho Run/Debug NetBeans: chỉ khi có đủ bộ MySQL và cấu hình private của workspace, hook chạy trực tiếp `tools/StartLocalDatabase.java` bằng Java của Ant (không cần pwsh hoặc thay đổi ExecutionPolicy) rồi chờ MySQL trả lời tối đa 30 giây. Không khởi tạo lại hoặc xóa database, không thay đổi dịch vụ Windows hay SQL Server.

Có thể chạy riêng `ant start-local-database` trước khi mở website. Khi mở URL trực tiếp mà không Run NetBeans sau khi khởi động máy, vẫn cần bật MySQL trước. Bản WAR triển khai độc lập dùng vòng đời database của môi trường đích.

Đã tái hiện MySQL tắt/Tomcat bật, sau đó kiểm thử hook khởi động từ trạng thái MySQL dừng: Ant thành công, trang chủ HTTP 200 và 5/5 kiểm tra tài khoản đạt. Kiểm thử HTTP hồi quy ghi nhận bên dưới trong báo cáo phiên làm việc.

Giữ nguyên Java 17, Ant, NetBeans 17 và Jakarta Servlet/JSP. Project hiện khai báo Servlet 6 và dùng Tomcat **10.1**; Tomcat 10.0 chỉ có Servlet 5 nên không phải cấu hình đã kiểm thử.

MySQL và SQL Server là hai hệ quản trị khác nhau. SSMS 19 không quản lý MySQL. Có thể giữ SQL Server/SSMS trên máy và chạy MySQL ở cổng riêng.

Nguồn tải: [MySQL Community Server](https://dev.mysql.com/downloads/mysql/8.4.html), [Workbench](https://dev.mysql.com/downloads/workbench/), [Connector/J](https://dev.mysql.com/downloads/connector/j/). Môi trường kiểm thử dùng MySQL 8.4.0 ZIP, Connector/J 8.4.0 và Tomcat 10.1.17. Khi cài môi trường khác, chọn bản vá MySQL 8.4 đang được hỗ trợ.

## 1. Tạo database bằng Workbench

1. Cài MySQL Community Server 8.4 và Workbench. Dùng mật khẩu quản trị riêng trong quá trình cài.
2. Mở Workbench, tạo kết nối tới host/cổng MySQL. Trên máy hiện tại, MySQL riêng của dự án là `127.0.0.1:3307`; cài đặt thông thường có thể là `3306`.
3. **File → Open SQL Script**, mở `database/smartstu.sql`. Chạy toàn bộ script bằng biểu tượng tia sét. Refresh Schemas.
4. Script không DROP/TRUNCATE bảng hay xóa dữ liệu. Migration v2 được kiểm tra từng cột, index và constraint trước khi thêm. Trên database v1 đang có dữ liệu, có thể chỉ chạy `database/migrations/002-integrity.sql` sau khi sao lưu.
5. Tạo tài khoản JDBC riêng, chỉ cấp `SELECT, INSERT, UPDATE, DELETE` trên `smartstu.*`. Dùng tài khoản quản trị để chạy DDL/migration; không chạy web bằng root.

```sql
-- Thực hiện trong Workbench, thay mật khẩu bằng giá trị riêng của bạn.
CREATE USER 'smartstu'@'127.0.0.1' IDENTIFIED BY 'MAT_KHAU_RIENG_DO_BAN_DAT';
GRANT SELECT, INSERT, UPDATE, DELETE ON smartstu.* TO 'smartstu'@'127.0.0.1';
SHOW TABLES FROM smartstu;
SELECT * FROM smartstu.schema_migrations;
```

Không chạy lại CREATE USER để đổi mật khẩu người dùng đang có. Nếu đã có user, dùng user đó với quyền phù hợp.

## 2. JDBC driver và cấu hình bí mật

Driver đã đặt ở `web/WEB-INF/lib/mysql-connector-j-8.4.0.jar`, Ant đóng gói vào WAR. JDBC dùng `java.sql` và `javax.sql.DataSource`; đây là API JDBC đúng, không đổi thành `jakarta.sql`.

`Database.java` đọc cấu hình theo thứ tự: Java system property → biến môi trường → file properties được chỉ định bằng `SMARTSTU_CONFIG_FILE`.

```properties
SMARTSTU_STORAGE=mysql
SMARTSTU_DB_URL=jdbc:mysql://127.0.0.1:3306/smartstu?connectionTimeZone=UTC&characterEncoding=UTF-8&connectTimeout=5000&socketTimeout=10000
SMARTSTU_DB_USER=smartstu
SMARTSTU_DB_PASSWORD=MAT_KHAU_RIENG_DO_BAN_DAT
```

Lưu file ngoài `web`, ngoài WAR và ngoài Git. Không đưa credentials vào URL. Mỗi connection được đặt session UTC để thời gian MySQL và Java nhất quán; giao diện định dạng giờ Việt Nam/Thái Lan. Tài khoản MySQL không có quyền DDL vẫn đặt được múi giờ session.

Trong cấu hình Tomcat ở NetBeans, thêm VM option:

**Riêng workspace hiện tại:** khi chạy bản exploded tại `build/web/WEB-INF/classes`, ứng dụng xác minh `nbproject/project.xml` rồi tự đọc `.runtime/db.local.properties` bên ngoài thư mục public. Không cần VM option cho F6. File private này không được đóng gói vào WAR. Cấu hình tự phát hiện dùng JDBC trực tiếp vì Tomcat NetBeans không có Resource của máy chủ kiểm thử riêng. Cấu hình tường minh bằng property/biến môi trường vẫn được ưu tiên; triển khai WAR độc lập cần chỉ định file như dưới đây.

```text
-DSMARTSTU_CONFIG_FILE=C:/duong-dan-rieng/smartstu.local.properties
```

Khởi động lại Tomcat sau khi đổi cấu hình. Không chỉ reload trang. Nếu cấu hình MySQL thiếu/lỗi, ứng dụng báo dịch vụ dữ liệu không khả dụng; không tự chuyển sang dữ liệu giả.

## 3. Khởi tạo tài khoản và học liệu

Không tự sinh tài khoản/mật khẩu công khai khi Tomcat khởi động. Có hai cách **tùy chọn**, chỉ cần dùng một cách trên database mới:

### Cách A: Java initializer với mật khẩu của bạn

Thêm `SMARTSTU_SEED_PASSWORD` từ 12–200 ký tự vào file riêng. Sau khi Ant build:

```powershell
& 'C:/Program Files/Java/jdk-17.0.20/bin/java.exe' `
  '-DSMARTSTU_CONFIG_FILE=C:/duong-dan-rieng/smartstu.local.properties' `
  '-DSMARTSTU_DB_JNDI=' `
  -cp 'build/web/WEB-INF/classes;web/WEB-INF/lib/*' `
  com.smartstudy.persistence.SeedDatabase
```

Tạo `student@smartstudy.vn`, `teacher@smartstudy.vn`, `admin@smartstudy.vn` với mật khẩu đã đặt, băm PBKDF2-HMAC-SHA256, salt ngẫu nhiên và 120.000 vòng. Initializer giữ nguyên các nhóm dữ liệu đã tồn tại; không dùng initializer để đặt lại mật khẩu tài khoản đang có.

### Cách B: SQL fixture cục bộ

Sau schema, mở `database/seed-local.sql` trong Workbench và chạy. File chứa tài khoản băm và học liệu khởi tạo sạch, không chứa bài nộp/nhật ký của người dùng. `INSERT IGNORE` không ghi đè bản ghi cũ. Các hash của file hiện tại khớp mật khẩu khởi tạo riêng trên máy kiểm thử, không phải `123456`. Trên máy khác, nên dùng cách A hoặc tạo lại SQL bằng `test/SeedSqlExporter.java` với mật khẩu riêng.

Trên máy hiện tại, mật khẩu khởi tạo được lưu riêng trong `.runtime/db.local.properties` tại dòng `SMARTSTU_SEED_PASSWORD`. Không chia sẻ file này. Tài khoản Student dùng cho kiểm tra trình duyệt là fixture riêng và sẽ được khóa sau kiểm thử.

## 4. Connection pool

Chế độ JDBC trực tiếp mở/đóng connection bằng try-with-resources. Để dùng pool Tomcat, khai báo Resource `jdbc/SmartStu` trong context **ngoài WAR**, đặt driver trong `CATALINA_BASE/lib` và cấu hình:

```properties
SMARTSTU_DB_JNDI=java:comp/env/jdbc/SmartStu
```

Resource dùng `org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory`, `maxTotal=12`, `maxIdle=6`, `minIdle=1`, `maxWaitMillis=5000`, `validationQuery=SELECT 1`, `testOnBorrow=true`. Credentials chỉ ở context ngoài Git. Máy hiện tại đã cấu hình pool bằng `tools/configure-local-pool.ps1`.

## 5. Bố trí dữ liệu và MVC/DAO

- Controller nhận request, kiểm tra CSRF/session; service giữ validation và quyền sở hữu; DAO dùng PreparedStatement; JSP hiển thị dữ liệu. Giao diện cũ được giữ nguyên, chỉ thêm trang Tutor và cập nhật mô tả lưu trữ.
- `JdbcMap<V>` là DAO có kiểu cho từng aggregate hiện có, không phải cache bộ nhớ dùng thay database. Mỗi nhóm có bảng riêng, ID ổn định, JSON payload và cột SQL sinh để lập index/kiểm tra quan hệ. Dữ liệu đọc trong request có cache phạm vi request và bị xóa khi có mutation; không giữ dữ liệu cũ qua các request.
- `catalog_entries` giữ mô hình nội dung thống nhất của dự án. `courses`, `lessons`, `flashcards` là **view SQL**, không có bản nội dung thứ hai bị lệch nhau.
- `quizzes`, `quiz_runs`, `quiz_attempts` lưu cả phiên bản câu hỏi thực sự đã giao. `quiz_questions` là view `JSON_TABLE` để truy vấn câu trong quiz.
- Tài khoản và thông tin băm ở `users`, `user_credentials`; khóa, vai trò, thông tin cá nhân đều lưu bền vững.
- Chương, vị trí học liệu, đăng ký, tiến độ bài học/thẻ, thời gian, lịch sử, quiz, đề/rubric/nháp/bài nộp/lịch sử chấm, nguồn và phiên bản ngân hàng câu hỏi, audit và chính sách AI đều lưu MySQL.
- Điểm quiz có CHECK theo thang điểm. Khóa ngoại bảo vệ chủ sở hữu, đăng ký, chương, quiz/run, bài nộp, nháp, gợi ý chấm và hội thoại. Hai view `student_lesson_progress`, `student_flashcard_progress` tách các ID trong aggregate để báo cáo SQL.
- `ConversationDao` dùng bảng quan hệ `ai_conversations`/`ai_messages`, index theo owner và thứ tự tin, unique `(conversation_id, request_id, role)`. Chủ sở hữu được kiểm tra ngay trong DAO. Ẩn hội thoại là soft delete.
- Danh sách hội thoại phân trang ở server, 50 hội thoại/trang; nội dung giới hạn 50 lượt/hội thoại. Nhật ký truy vấn tối đa 1.000 sự kiện gần nhất để hiển thị, dữ liệu cũ không bị tự xóa.
- POST nhiều thay đổi được thực hiện trong transaction. Khóa `write_lock` tuần tự hóa thao tác ghi của các service hiện tại; chỉ gửi redirect/body thành công sau commit. Các lệnh gọi AI không giữ transaction/connection trong lúc chờ mạng.
- `pulses` đo nhịp đọc bài, session đang hoạt động và cửa sổ giới hạn tần suất AI là trạng thái tạm thời; số giây học đã ghi nhận, bài làm và kết quả được lưu bền vững.

Đây là thiết kế kết hợp bảng quan hệ và aggregate JSON nhằm giữ đúng phiên bản bài kiểm tra/rubric và tương thích frontend/service hiện có. Không phải schema mọi thuộc tính đều tách thành cột/bảng 3NF. Chưa kiểm thử tải lớn hoặc triển khai nhiều nút; khóa ghi toàn cục ưu tiên tính toàn vẹn cho mô hình Tomcat hiện tại.

## 6. Chạy trên máy hiện tại

```powershell
pwsh -NoProfile -File tools/start-local-mysql.ps1
pwsh -NoProfile -File tools/start-local-tomcat.ps1
```

Mở `http://localhost:8081/SmartStudy/home`. Trong NetBeans: chọn JDK 17 và Tomcat 10.1, đặt cấu hình bên ngoài như trên, Clean and Build rồi Run. Nếu Tomcat kiểm thử đang giữ cổng 8081, tắt **đúng tiến trình kiểm thử** trước khi chạy NetBeans:

```powershell
pwsh -NoProfile -File tools/stop-local-tomcat.ps1
```

Muốn tắt MySQL riêng của dự án:

```powershell
pwsh -NoProfile -File tools/stop-local-mysql.ps1
```

Script chờ MySQL flush và thoát hoàn toàn trước khi bạn chạy lại. Không xóa `.runtime/mysql-data` để restart. Không khởi tạo lại data directory đã có dữ liệu.

## 7. Kiểm tra và sao lưu

`tools/run-db-regression.ps1` đọc mật khẩu fixture từ file riêng, kiểm thử ba vai trò bằng HTTP. `DatabaseIntegrationTest` kiểm tra CRUD, rollback, hội thoại và chống gửi trùng; `RelationalWorkflowTest` kiểm tra khóa học → chương → bài học → ngân hàng → quiz → tự luận/rubric trong transaction rollback. `tools/check-schema.ps1` kiểm tra schema/seed trên database tên riêng rồi giữ lại để xem xét.

Sao lưu bằng Workbench Data Export hoặc mysqldump với `--single-transaction --no-tablespaces`. Không đặt mật khẩu trực tiếp vào command line. Trước migration v2, bản dữ liệu kiểm thử đã được lưu riêng tại `.runtime/db-before-v2.sql`.
