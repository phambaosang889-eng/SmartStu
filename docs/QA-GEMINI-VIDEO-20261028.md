# Bàn giao Gemini, video và giao diện — 28/10/2026

## Đã triển khai

- Gemini REST trực tiếp bằng Java HttpClient cho tutor, tạo câu hỏi và hỗ trợ chấm rubric. Key riêng ngoài WAR, HTTPS host cố định, timeout, giới hạn response, structured schema và validation nghiệp vụ. Gateway cũ giữ tương thích.
- Teacher vẫn phải chọn/lọc/sửa câu hỏi và duyệt điểm, không phát hành/công bố tự động. Trang tạo câu hỏi dùng Gemini khi đủ cấu hình, giữ lại phạm vi/mục tiêu/số câu của lần tạo trước trong session.
- Bốn video YouTube đã tra nguồn và gắn bài học; có ghi tác giả/đơn vị, mở nguồn dự phòng. Không tải hoặc tái đăng video của bên khác.
- Giáo viên upload MP4/WebM 200 MB/video qua multipart, giới hạn 2 GB/tài khoản, kiểm tra chữ ký/đuôi file, CSRF/ownership. Metadata MySQL, file UUID ngoài WAR; học sinh được cấp quyền mới xem được. HTTP Range, suffix range, HEAD, 416, soft archive.
- Giữ tông xanh hiện có; tăng cỡ chữ menu/nút/nội dung, đồng bộ form, khung video 16:9 và khu quản lý hai lựa chọn. Thêm lối tắt tới phần video. Kiểm tra giao diện desktop và sáng/tối; sửa encoding UTF-8 các JSP include mới.

## Kết quả kiểm thử thực chạy

| Nhóm | Kết quả |
|---|---|
| Ant dist | Thành công |
| Jasper compile toàn bộ JSP | 0 lỗi |
| Gemini thật, nội dung kiểm thử tổng hợp | 3/3: tutor, 2 câu hỏi, bài tự luận 3 tiêu chí có evidence đúng nguyên văn |
| Gemini request/response fixtures | 20 kiểm tra đạt, không phải API thật |
| Video service/DAO | 24 kiểm tra đạt, gồm học sinh chưa đăng ký, bài nháp và teacher ngoài phạm vi; rollback SQL |
| Upload MP4/CSRF/định dạng/HTTP streaming | 16 kiểm tra đạt |
| Hồi quy HTTP Student/Teacher/Admin | 239 kiểm tra đạt; tạm dừng AI trong đoạn test thiếu cấu hình rồi khôi phục, không gọi API ngoài ý muốn |
| Đăng ký/đăng nhập | 24 kiểm tra đạt |
| Schema mới + seed chạy hai lần | Đạt; 31 bảng, 6 view, 34 FK; 31 kiểm tra SQL nghiệp vụ rollback đạt |
| Kiểm tra WAR | Không chứa file private, thư mục runtime hoặc API key Gemini thật |

Trình duyệt đã thực hiện: Student xem video YouTube Newton (trình phát chạy tới 00:36); MP4 upload clip kiểm thử 5.055 giây readyState=4, không lỗi decoder, phát bằng bàn phím; gửi câu hỏi tutor nhận kết quả a=5 m/s² cùng hướng hợp lực; Teacher tạo hai câu thật bằng Gemini để xem trước; xem form quản lý video và kiểm tra theme tối/sáng. Không ghi nhận console warning/error trên lượt tutor. Clip QA upload đã được ẩn, không dùng làm học liệu. Câu và điểm của live service test không được công bố.

Ảnh bằng chứng trong `docs/screenshots/`:

- `lesson-video-desktop-20261028.png`
- `gemini-live-desktop-20261028.png`
- `teacher-video-desktop-20261028.png`

## Giới hạn thực tế

Chưa kiểm thử tải nhiều người cùng upload/gọi AI; mô hình ghi hiện tại dùng write lock chung. Không có chuyển mã video/quét virus; MP4 H.264/AAC và WebM cần được giáo viên chuẩn bị đúng. Video YouTube phụ thuộc quyền nhúng/khả dụng của nguồn ngoài. Chức năng ẩn giữ file và metadata, chưa có giao diện khôi phục/purge. Khi lỗi commit sau khi ghi file có thể còn orphan, chưa có tác vụ dọn tự động. AI có thể sai kiến thức; giáo viên phải rà soát đáp án, đơn vị, rubric và điểm.

Xem [GEMINI.md](GEMINI.md), [LESSON-VIDEOS.md](LESSON-VIDEOS.md), [DATABASE.md](DATABASE.md) để cấu hình/vận hành. Không cần đổi mật khẩu tài khoản khởi tạo cho lần triển khai này.
