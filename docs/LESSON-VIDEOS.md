# Video bài giảng và lưu trữ

## Dùng trên website

Teacher → Bài học → Chỉnh sửa. Sau form nội dung có **Video bài giảng**:

- Gắn video YouTube: nhập tên, liên kết HTTPS của video và tác giả/nguồn. Video hiển thị trong bài học bằng youtube-nocookie, có liên kết mở nguồn khi không xem nhúng được. Không tải xuống hoặc đăng lại video của đơn vị khác.
- Tải video của mình: tên, file MP4/WebM và xác nhận quyền sử dụng. Tối đa 200 MB/file, 2 GB/tài khoản giáo viên (tính cả file ẩn vì vẫn chiếm dung lượng). MP4 nên dùng H.264/AAC để tương thích trình duyệt. Server kiểm tra phần mở rộng và chữ ký file; không chuyển mã hoặc quét virus.
- Ẩn video khỏi bài học: soft archive; dữ liệu và file được giữ, không xóa vĩnh viễn. Chưa có giao diện khôi phục video đã ẩn; quản trị viên có thể hỗ trợ qua metadata sau kiểm tra quyền.
- Phải lưu bài học mới trước, rồi mở Chỉnh sửa để thêm video. Lưu nội dung và thêm video là các thao tác riêng, có nhắc trên giao diện.

Học sinh chỉ xem video của bài đã xuất bản trong khóa đã tham gia và đang xuất bản. Teacher chỉ quản lý bài thuộc mình. Admin được xem để kiểm duyệt. Endpoint stream kiểm tra quyền mỗi lần, hỗ trợ GET Range (206/416) và HEAD, không công khai đường dẫn file.

## Database và file

- Chạy `database/migrations/003-lesson-videos.sql` trên database hiện tại; migration chỉ thêm bảng/index/FK, không xóa nội dung.
- Schema tổng `database/smartstu.sql` đã gồm migration này cho cài mới.
- `lesson_videos` lưu liên kết bài học, chủ sở hữu, nguồn, tên file UUID, MIME, dung lượng và trạng thái. DAO dùng PreparedStatement.
- Byte video ở thư mục ngoài WAR do `SMARTSTU_VIDEO_DIR` trong file private xác định. Workspace hiện dùng `.runtime/lesson-videos` với đường dẫn tuyệt đối. Môi trường khác mặc định ở `user.home/.smartstu/lesson-videos`; nên đặt đường dẫn riêng có quyền ghi cho Tomcat.
- Sao lưu đồng thời MySQL và thư mục video. Redeploy WAR không xóa file. Không đặt thư mục video vào `web`, không commit video/private config.
- File của lần upload có lỗi ghi/validation được dọn ngay; nếu filesystem thành công nhưng JDBC commit sau đó lỗi, có thể còn file UUID không có metadata. Chưa có tác vụ dọn orphan tự động. File ẩn vẫn được giữ để không làm mất dữ liệu.
- Không giữ connection trong lúc phát video. Upload POST dùng transaction có khóa ghi hiện tại; mô hình này phù hợp triển khai cục bộ, chưa kiểm thử tải upload đồng thời nhiều người.

## Bốn nguồn đã gắn

Nguồn được xác minh ngày 27/10/2026, chỉ nhúng/liên kết công khai. Nội dung bổ trợ kiến thức, số bài trong video có thể khác bộ sách đang học.

| Bài học SmartStu | Nguồn |
|---|---|
| Ba định luật Newton | [VietJack THPT Official · Cô Nguyễn Mai Phương](https://www.youtube.com/watch?v=gFJgWqZV-aQ) |
| Chuyển động thẳng biến đổi đều | [VietJack · Thầy Lê Xuân Vượng](https://www.youtube.com/watch?v=-scFOvSVvkQ) |
| Công và năng lượng | [Công và công suất · VietJack](https://www.youtube.com/watch?v=-zafKyYDuSo) |
| Lực ma sát trượt | [Lực ma sát · VietJack](https://www.youtube.com/watch?v=MOSsaEyPLBs) |

`database/lesson-video-sources.sql` chỉ thêm khi bài học seed tồn tại và chưa có cùng nguồn, không ghi đè. Đã chạy lại hai lần không tạo trùng. Nguồn ngoài có thể thay đổi hoặc ngừng cho phép nhúng.

## Kiểm thử

- `test/LessonVideoTest.java`: YouTube URL whitelist, chống URL đánh lừa/user-info, chữ ký file, sở hữu, soft archive và SQL rollback.
- `tools/test-lesson-video.ps1`: multipart MP4 thật, CSRF/role, file giả, exact byte count, range/suffix/416, HEAD và HTML player.
- Fixture MP4 từ [ví dụ video của MDN](https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/video), URL `https://developer.mozilla.org/shared-assets/videos/flower.mp4`, chỉ ở `.runtime/video-qa.mp4`, không phải bài giảng. Clip QA upload được ẩn sau khi kiểm tra phát trong trình duyệt, file vẫn giữ.
- Trình duyệt đã đọc clip 5.055 giây, readyState=4, không lỗi decoder và bắt đầu phát bằng bàn phím. Trình phát YouTube Newton đã tải metadata 15 phút 50 giây.
