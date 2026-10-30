# Dọn dự án và giao diện điện thoại — 28/10/2026

## Các mục đã dọn

Đã kiểm tra tham chiếu và lưu `backups/cleanup-unused-20261028.zip` trước khi xóa sáu mục sau khỏi thư mục làm việc:

| Mục | Lý do |
|---|---|
| `.compile-check/` | Class kiểm thử và bản sao class ứng dụng đã sinh, có thể biên dịch lại; loại bỏ nguy cơ nạp nhầm class cũ |
| `.jsp-check/` | Java/class do Jasper sinh, có thể tạo lại |
| `tools/connect-stores.ps1` | Script chuyển đổi source sang persistence ở giai đoạn triển khai cũ; không dùng khi build/run |
| `tools/migrate-shell.ps1` | Script ghi lại JSP trong giai đoạn chuyển giao diện trước; không dùng khi vận hành |
| `tools/write-migration.ps1` | Bộ tạo migration cũ; SQL thành phẩm đã có trong database/migrations |
| `tools/write-schema.ps1` | Bộ sinh schema cũ, không còn bao quát migration video mới; schema thành phẩm được giữ |

Không xóa source, JSP cũ chưa chứng minh được là không cần, driver JDBC, cấu hình NetBeans, tài liệu vận hành, backup, database, cấu hình riêng hoặc video. `build/web` đang là docBase Tomcat nên được giữ. `dist` là WAR bàn giao.

`tools/compile-checks.ps1` biên dịch toàn bộ `test/*.java` vào `.runtime/test-classes` với class ứng dụng ở `build/web/WEB-INF/classes` đi trước trong classpath. `check-schema.ps1` đã cập nhật đường dẫn và tự gọi bước biên dịch. Jasper QA từ nay xuất vào `.runtime/jsp-check`, không tạo thư mục kiểm thử ở root.

## Responsive đã bổ sung

- CSS riêng `web/assets/css/mobile.css` dùng chung, giữ bố cục desktop. Áp dụng cho trang chủ, đăng nhập, đăng ký và toàn bộ workspace ba vai trò.
- Header gọn và cố định khi cuộn; menu trượt có nút đóng, backdrop, Escape, giữ focus trong menu và khóa nội dung nền. Menu đóng khi chọn trang hoặc đổi breakpoint.
- Dashboard hai cột chỉ số; học liệu, form và video một cột. Bảng dài cuộn trong vùng bảng, không kéo rộng cả trang.
- Ô nhập 16 px, nút ít nhất 44 px, đáp án quiz có vùng nhấn lớn, chữ/nội dung tự xuống dòng.
- Video giữ tỷ lệ 16:9, tác giả và liên kết nguồn xuống dòng; form upload giữ đủ các trường.
- AI có lịch sử thu gọn trên điện thoại, khung tin nhắn không cuộn lồng quá nhiều; ô nhập và nút gửi bố trí dọc.
- Nút submit được khôi phục khi quay lại trang qua bfcache; không để trang cũ bị kẹt ở trạng thái đang lưu.

## Kiểm thử thực chạy

- Build Ant thành công; Jasper toàn bộ JSP 0 lỗi.
- 239 kiểm tra HTTP; 24 đăng ký/đăng nhập; 53 nghiệp vụ memory; 20 contract Gemini đạt. Không gọi thêm Gemini thật trong lượt responsive.
- Trình duyệt: trang chủ/đăng nhập/dashboard, menu mở/đóng bằng Escape, chuyển trang từ menu, bài học có video ở 320 px, lịch sử AI mở/thu, quiz chọn 3 đáp án và nộp kết quả 100/100, form video Teacher một cột.
- Đã đo tràn ngang trên các trang được chọn ở 320/360/390/414/768/1440 px: document width không vượt viewport. Ở quiz 390 px, vùng đáp án khoảng 54,7 px cao. Ô nhập AI tính ra 16 px.
- Sau thay đổi breakpoint, sidebar desktop trở lại và phần nền không còn bị khóa.
- Admin đã đăng nhập ở 360 px; hai vùng bảng có chiều rộng hiển thị 287 px và cuộn nội bộ, document rộng 345 px trong viewport 360 px. Trang đăng ký cũng không tràn ngang.

Ảnh `docs/screenshots/mobile-dashboard-20261028.png` và `mobile-quiz-20261028.png`. Đây là kiểm thử viewport trên trình duyệt desktop, chưa phải kiểm thử trực tiếp trên iPhone/Android thật, bàn phím ảo hoặc đường truyền 4G.
