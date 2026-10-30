# Kết nối Google Gemini trực tiếp

SmartStu gọi Gemini từ Java 17 HttpClient, không cần gateway, SDK frontend hoặc thay đổi công nghệ. Adapter dùng generateContent REST và structured JSON; giữ validation phía server và bước giáo viên duyệt câu hỏi/điểm.

## Cấu hình

Trong file private `.runtime/db.local.properties` của workspace hiện tại, giữ các dòng database và thêm:

```properties
SMARTSTU_AI_PROVIDER=gemini
SMARTSTU_GEMINI_MODEL=gemini-3.5-flash-lite
GEMINI_API_KEY=KEY_RIENG_CUA_BAN
```

Không commit hoặc chia sẻ file này, không đặt key trong JSP/JavaScript. Key được gửi trong header x-goog-api-key tới duy nhất generativelanguage.googleapis.com; không đưa vào URL/log. Có thể dùng biến môi trường thay file. Bản WAR triển khai máy khác vẫn cần SMARTSTU_CONFIG_FILE ngoài WAR như DATABASE.md.

Sau khi sửa key/model, reload webapp hoặc khởi động lại Tomcat. Admin → Trạng thái AI để bật chính sách và xem nhà cung cấp, số yêu cầu, lỗi. Đủ cấu hình không có nghĩa đã kiểm chứng kết nối; chỉ sau phản hồi thật mới ghi nhận thành công. Không có nút trừ quota chạy ngầm.

Nếu mô hình không được cấp quyền hoặc đã đổi tên, cập nhật SMARTSTU_GEMINI_MODEL theo danh sách trong Google AI Studio. Không tự đổi sang mô hình khác khi gặp lỗi vì có thể thay đổi chi phí.

## Xử lý và giới hạn

- Hỏi đáp dùng ngữ cảnh tối đa 12 tin/16.000 ký tự, câu trả lời tối đa 12.000 ký tự.
- Tạo câu hỏi theo phạm vi, mục tiêu, độ khó, thời gian và yêu cầu bổ sung. Chỉ nhận câu có 4 phương án khác nhau, chỉ số đáp án hợp lệ, lời giải, không trùng. Giáo viên chọn/sửa trước khi đưa vào ngân hàng.
- Chấm theo đúng số/thứ tự rubric và giới hạn điểm từng tiêu chí, kiểm tra evidence thực sự xuất hiện trong bài làm, confidence 0–1. Không gửi tên/email/ID học sinh trong payload chấm và không công bố điểm tự động.
- Timeout 25 giây, không redirect, body phản hồi tối đa 64 KB, giới hạn yêu cầu theo tài khoản trong 5 phút. Không tự retry để tránh trừ quota nhiều lần.
- Lỗi 400/401/403/404/429/5xx, safety block, MAX_TOKENS, JSON hỏng/thiếu trường đều có thông báo; không thay bằng nội dung hoặc điểm giả. Khi vượt token, giảm số câu/nội dung và thử lại.
- Hội thoại có thể chứa dữ liệu người dùng tự nhập. Chỉ gửi nội dung học tập cần thiết tới Google.

## Kiểm thử

`test/GeminiAdapterTest.java`: fixture cho định dạng request, auth header, URL cố định, role, schema, safety, JSON cắt/trùng/oversize, thought parts; không dùng quota thật.

`test/GeminiLiveTest.java`: chủ động gọi API thật bằng nội dung Vật lý tổng hợp: tutor, 2 câu Newton và một bài 3 tiêu chí. Không lưu câu vào quiz và không công bố điểm. Đã chạy đạt 3/3 với key private ngày 27/10/2026.

Tham khảo chính thức: [REST generateContent](https://ai.google.dev/api/generate-content), [Structured outputs](https://ai.google.dev/gemini-api/docs/structured-output), [mô hình đang dùng](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite).
