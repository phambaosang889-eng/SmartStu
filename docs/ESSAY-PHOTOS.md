# Nộp bài tự luận bằng ảnh

Học sinh mở bài tự luận, chọn ảnh hoặc mở máy ảnh, kiểm tra hướng ảnh rồi tải lên. Có thể nộp ảnh trực tiếp hoặc chọn Đọc ảnh bằng AI, kiểm tra bản nhận dạng và chèn vào câu trả lời. Giáo viên xem ảnh gốc, yêu cầu AI đề xuất điểm theo rubric, sửa và công bố kết quả.

Mỗi lần nộp hỗ trợ một ảnh JPEG/PNG tối đa 8 MB. Server kiểm tra định dạng thực, giới hạn 25 triệu điểm ảnh, chuẩn hóa JPEG tối đa 2 MB và loại metadata. Tổng dung lượng ảnh mỗi học sinh giới hạn 50 MB. Ảnh được lưu trong bảng essay_photos và đi cùng bản sao lưu MySQL. Chạy migration database/migrations/005-essay-photos.sql cho cơ sở dữ liệu cũ.

Camera cần quyền của trình duyệt và HTTPS hoặc localhost. Có thể chọn ảnh có sẵn nếu thiết bị không có camera. Không gửi ảnh có thông tin cá nhân không cần thiết. Nhận dạng cần Gemini được cấu hình; ảnh mờ hoặc dịch vụ lỗi vẫn có thể được giáo viên chấm thủ công. Điểm AI không tự công bố.

## Kiểm chứng ngày 30/10/2026

- Ant dist và biên dịch JSP thành công.
- 55 kiểm tra nghiệp vụ, 23 kiểm tra Gemini adapter, 24 kiểm tra dịch vụ ảnh thành công.
- 25 kiểm tra HTTP qua ứng dụng đang chạy, bao gồm Gemini thật đọc và chấm ảnh tổng hợp có lỗi tính toán, phân quyền ảnh, CSRF, từ chối ảnh giả, chống nộp trùng và giữ điểm chờ duyệt.
- Bài nộp kiểm thử tổng hợp: 3ae02cfb-9a04-4ea8-a5d3-f448b3e98fb0. Không phải bài của học sinh thật.
- Chưa xác minh thao tác chụp từ camera vật lý hoặc giao diện trên trình duyệt trong lượt kiểm thử này.

Biểu tượng đổi nền đã chuyển sang SVG cố định, không phụ thuộc ký tự biểu tượng dễ lỗi encoding. Cần kiểm tra trực quan sau khi tải lại trang.
