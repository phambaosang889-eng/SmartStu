# Hiển thị câu trả lời AI — 28/10/2026

Nguyên nhân: JSP đã escape nội dung đúng nhưng hiển thị nguyên văn Markdown và
LaTeX; trình duyệt không tự hiểu dấu ** hoặc lệnh phân số. Khoảng trắng pre-wrap
làm câu trả lời dài và khó đọc.

Thay đổi:
- KaTeX 0.19.1 kèm font và giấy phép, phục vụ trong ứng dụng, không CDN.
- tutor-format.js xử lý đoạn văn, danh sách, tiêu đề nhỏ, chữ đậm, mã nội dòng,
  công thức $...$, $$...$$, \\(...\\), \\[...\\].
- Chỉ phản hồi AI trạng thái complete được định dạng; câu hỏi và lỗi giữ dạng chữ.
- Không diễn giải HTML/link/ảnh từ AI. Công thức trust=false, maxSize=10,
  maxExpand=300; lỗi công thức trả về văn bản bằng textContent.
- Cỡ chữ hội thoại 15px, nhãn nhỏ, khoảng cách đoạn ngắn, công thức dài cuộn riêng.
- Gemini được yêu cầu dùng dấu phân cách LaTeX đúng bên trong chuỗi answer của JSON.

Xác minh đã thực hiện:
- Ant dist: BUILD SUCCESSFUL.
- Jasper toàn bộ JSP: 0 lỗi.
- GeminiAdapterTest: 20 kiểm tra fixture đạt.
- Đăng nhập Student trên Tomcat thật; gửi câu hỏi tốc độ 1 km / 10 phút.
- Câu trả lời sau cập nhật: 3 tên bước in đậm, 4 công thức KaTeX,
  0 fallback, kết quả khoảng 1,67 m/s; trang không tràn ngang ở viewport 1280.
- Console không có lỗi trong lượt kiểm tra trước cập nhật prompt.
- Ảnh: screenshots/tutor-format-20261028.png.

Nội dung cũ có dấu phân cách hợp lệ được định dạng khi mở lại. Công thức cũ thiếu
dấu phân cách hoặc sai cú pháp có thể vẫn hiện dạng chữ; không sửa nội dung đã lưu.
Đây là bộ định dạng Markdown giới hạn, không phải trình duyệt HTML/Markdown đầy đủ.

Sao lưu ba tệp giao diện trước sửa: backups/tutor-format-before-20261028.zip.
