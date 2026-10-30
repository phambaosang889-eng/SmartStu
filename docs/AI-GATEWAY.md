# Hợp đồng AI gateway SmartStu

Đây là hợp đồng gateway tùy chỉnh được giữ tương thích với bản cũ. Từ 27/10/2026, SmartStu đã có adapter Google Gemini trực tiếp; xem [GEMINI.md](GEMINI.md). Dùng `SMARTSTU_AI_PROVIDER=gemini` cho Gemini hoặc `SMARTSTU_AI_PROVIDER=gateway` cho endpoint HTTPS theo tài liệu này. Không đưa URL Gemini trực tiếp vào SMARTSTU_AI_ENDPOINT của chế độ gateway.

## Cấu hình phía máy chủ

- SMARTSTU_AI_ENDPOINT: URL HTTPS endpoint gateway.
- SMARTSTU_AI_KEY: khóa gửi trong header Authorization: Bearer. Không đặt khóa trong JSP, JavaScript, repo hoặc URL.
- Khởi động lại webapp sau khi đặt biến môi trường cho tiến trình Tomcat. Biến môi trường của terminal khác không tự áp dụng vào Tomcat đã chạy.
- Admin có thể cho phép/tạm dừng và đặt hạn mức 1–30 yêu cầu/5 phút/người dùng. Chính sách được lưu trong MySQL. Cửa sổ giới hạn tần suất áp dụng trong tiến trình Tomcat hiện tại.
- Có thể đặt các giá trị trong file bí mật bên ngoài ứng dụng qua `SMARTSTU_CONFIG_FILE`, như hướng dẫn DATABASE.md. Không đưa file này vào WAR/Git.

Yêu cầu POST application/json UTF-8. Chỉ chấp nhận HTTP 200, Content-Type application/json. Không theo redirect. Timeout toàn bộ phản hồi 25 giây, tối đa 64 KB, JSON sâu tối đa 20 cấp. Lỗi HTTP, mạng, timeout, JSON sai và rubric sai đều được báo trong trang; không thay bằng điểm ngẫu nhiên.

## Tạo câu hỏi

Payload gồm operation=questions, language=vi, topic, count (1–10), level, points, type=single_choice, lesson, objective, difficulty, secondsPerQuestion, additionalInstructions và reviewRequired=true.

Phản hồi:

```json
{"questions":[{"text":"Vật 2 kg chịu hợp lực 10 N. Gia tốc bằng bao nhiêu?","options":["2 m/s²","5 m/s²","10 m/s²","20 m/s²"],"correctIndex":1,"explanation":"a = F/m = 10/2 = 5 m/s², cùng hướng hợp lực."}]}
```

Cần 1 đến count câu, không trùng nội dung, mỗi câu đúng 4 phương án khác nhau, correctIndex nguyên 0–3, có lời giải. Backend kiểm tra cấu trúc và giới hạn dữ liệu; không thể tự chứng minh toàn bộ kiến thức Vật lý do mô hình tạo. Giáo viên phải rà soát công thức, đơn vị, số liệu và đáp án trước khi chọn lưu/phát hành. Đáp án sai cần được giải thích trong explanation khi phù hợp.

## Hỗ trợ chấm tự luận

Payload gồm operation=essay, language=vi, prompt, reference (do giáo viên cung cấp), rubric=[{title,maximum}], answer và reviewRequired=true. Không gửi email, tên hoặc ID học sinh.

Phản hồi phải có criteria theo đúng thứ tự rubric:

```json
{"criteria":[{"score":3,"feedback":"Công thức và phép biến đổi đúng.","evidence":"a = F/m"}],"strengths":"Trình bày đúng công thức.","errors":"Cần kiểm tra đơn vị trong kết luận.","improvements":"Viết rõ đơn vị và hướng gia tốc.","confidence":0.82}
```

Ví dụ chỉ minh họa cấu trúc; số phần tử criteria phải bằng số tiêu chí thật. Điểm mỗi tiêu chí trong [0, maximum], confidence trong [0,1], evidence phải là đoạn trích nguyên văn thực sự có trong bài làm. Nếu không đủ căn cứ, gateway cần giải thích hạn chế trong feedback/errors, giữ độ tin cậy phù hợp và luôn yêu cầu giáo viên xem lại. Không dựa vào thuộc tính nhạy cảm hoặc suy đoán về người học.

Đề xuất AI được lưu riêng với điểm giáo viên và không công bố tự động. Giáo viên kiểm tra, điều chỉnh từng tiêu chí và nhận xét, sau đó chọn công bố. Lịch sử chấm giữ các kết quả giáo viên đã lưu. Khi AI lỗi hoặc chưa cấu hình, chấm thủ công vẫn hoạt động.

## Trợ lý học tập (AI Tutor)

Student, Teacher và Admin có thể dùng hội thoại riêng của mình qua `/{role}/ai-tutor`. Admin không có quyền đọc hội thoại riêng chỉ bằng cách thay ID. SQL kiểm tra owner trong mọi thao tác đọc, ghi, hoàn tất yêu cầu và ẩn hội thoại.

Gateway nhận payload:

```json
{"operation":"tutor","language":"vi","messages":[{"role":"user","content":"Giải thích định luật II Newton"}],"maxOutputTokens":1000,"instructions":"Quy tắc trợ giảng do backend cung cấp"}
```

Gateway phải gọi mô hình thật, tuân thủ giới hạn đầu ra và trả về:

```json
{"answer":"Nội dung giải thích bằng tiếng Việt, có kiểm tra công thức và đơn vị."}
```

Trường `answer` phải là văn bản không rỗng, tối đa 12.000 ký tự; frontend hiển thị dưới dạng văn bản đã escape, không thực thi HTML. Phản hồi sai cấu trúc, lỗi mạng và timeout không được thay bằng câu trả lời mẫu.

Mỗi đầu vào tối đa 4.000 ký tự; ngữ cảnh tối đa 12 tin và 16.000 ký tự; timeout 25 giây và phản hồi tối đa 64 KB. Không gửi tên/email/ID tài khoản vào payload AI. Nội dung do người học tự nhập vẫn có thể chứa thông tin cá nhân; giao diện nhắc chỉ gửi dữ kiện học tập cần thiết.

Tin nhắn được lưu vào `ai_messages` theo cặp user/assistant có unique request ID. Yêu cầu mới claim bằng transaction ngắn, sau đó đóng connection trước khi gọi mạng. Kết quả được lưu riêng bằng transaction khác. Khi chưa cấu hình hoặc lỗi, câu hỏi và trạng thái thất bại vẫn được lưu; nút Gửi lại dùng cùng request ID nên không tạo tin trùng. Một hội thoại xử lý một yêu cầu tại một thời điểm. Yêu cầu bị gián đoạn có thể gửi lại sau hai phút. Giới hạn 50 lượt/hội thoại để giữ ngữ cảnh và chi phí hữu hạn.

Gateway tùy chỉnh chưa được kiểm chứng với dịch vụ ngoài. Adapter Gemini trực tiếp đã được kiểm thử thật cho hỏi đáp, tạo câu hỏi và chấm rubric bằng key riêng trên máy; xem GEMINI.md. Không dùng gateway để gọi trực tiếp giao thức Google.
