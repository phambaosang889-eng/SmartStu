# Rà soát cuối ngày 30/10/2026

- Chuẩn hóa 67 file Java bằng google-java-format 1.24.0, kiểu AOSP thụt lề 4 dấu cách. Giữ nguyên import và nghiệp vụ. Công cụ chỉ nằm trong .runtime, không được đóng gói vào ứng dụng.
- Thêm .editorconfig: UTF-8, dấu cách, newline cuối file; Java 4 dấu cách, JS/CSS 2 dấu cách.
- Sửa 9 JSP có chuỗi Java bị ngắt dòng bên trong dấu ngoặc kép: error, register, ai-settings, ai-tutor, dashboard, courses, accounts, catalog, content. Việc ngắt dòng HTML tự động không an toàn với Java scriptlet.
- Sao lưu Java trước format: backups/source-before-format-20261030.zip; sao lưu JSP trước sửa: backups/views-before-final-repair-20261030.zip.
- Ant dist thành công. Jasper biên dịch toàn bộ JSP: 0 lỗi.
- DomainWorkflowTest: 55 kiểm tra đạt trong JVM memory độc lập. GeminiAdapterTest: 23 kiểm tra hợp đồng phản hồi đạt, không gọi API thật.
- HTTP regression trên Tomcat/MySQL: 240 kiểm tra đạt sau sửa; đăng nhập ba vai trò, menu, CSRF, quiz, tự luận, hội thoại riêng tư, XSS, đăng xuất, khóa tài khoản và CSV. Bộ kiểm thử đọc token, liên kết và nhãn nút chấp nhận khoảng trắng/xuống dòng HTML.

Khi sửa tiếp: dùng formatter Java cho .java; không dùng formatter HTML tự động cho toàn bộ JSP có scriptlet. Kiểm tra Jasper sau khi sửa JSP vì Ant dist không tự bảo đảm tất cả trang JSP biên dịch được.

Hướng dẫn giải thích backend và kịch bản trình bày nằm trong BACKEND-REVIEW.md.
