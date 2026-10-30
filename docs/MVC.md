# Cấu trúc MVC của SmartStu

SmartStu giữ mô hình Servlet/JSP MVC, không bổ sung framework hoặc thay nền tảng.

- `author`: xác thực, phiên đăng nhập và kiểm tra vai trò.
- `web/*Servlet`: điểm vào HTTP theo vai trò.
- `web/*Controller`: đọc/kiểm tra request, gọi service, chuẩn bị dữ liệu và forward hoặc redirect.
- `service`: quyền sở hữu, nghiệp vụ học tập, chấm điểm, báo cáo và kết nối AI.
- `model`: dữ liệu nghiệp vụ; `model/view`: dữ liệu trình bày bất biến.
- `WEB-INF/views`: JSP hiển thị dữ liệu và biểu mẫu; `assets`: CSS, icon và tương tác phía trình duyệt.

## Phần được chuẩn hóa trong lượt chỉnh giao diện

`LayoutFilter` chạy khi forward đến trang riêng tư, chuẩn bị `LayoutModel`: người dùng, tiêu đề, mục điều hướng đang chọn, icon và thông báo. Các fragment dùng chung đọc model này thay vì tự xây menu hoặc thay đổi session.

`DashboardService` tính các chỉ số, chuẩn hóa chiều cao cột biểu đồ và chọn bài học tiếp theo. Các DTO `DashboardMetric`, `ChartBar`, `GrowthChart` nằm trong model/view. Dashboard JSP không còn gọi repository/service để truy vấn dữ liệu hoặc tính các chỉ số này, và không phụ thuộc lớp Controller để lấy kiểu dữ liệu.

`QuestionController` chuẩn bị trạng thái AI và thông tin phiên bản/lưu trữ câu hỏi. `AIController` cung cấp `AIStatus`; các JSP tương ứng chỉ hiển thị trạng thái được chuẩn bị.

Đây là refactor tập trung vào layout, dashboard và trang hỗ trợ câu hỏi. Một số view cũ ở module khác vẫn có scriptlet và truy vấn dữ liệu hiển thị; chưa phải chuyển toàn bộ JSP sang JSTL/EL. Các kiểm tra quyền và thay đổi nghiệp vụ vẫn thực hiện ở Java phía server.

## Nội dung giao diện

Thông tin triển khai và giới hạn lưu trữ được giữ trong trang thông tin hệ thống, phần thông tin phiên bản và tài liệu dự án. Không lặp lại ở sidebar/footer. Nguồn câu hỏi mẫu hiển thị là “Ngân hàng tham khảo”; trang chuẩn bị câu hỏi vẫn ghi đúng trạng thái AI chưa kết nối, không quảng bá dữ liệu tham khảo là kết quả AI.

`workspace.css` cung cấp cấu trúc và component; `refinement.css` là lớp tinh chỉnh thẩm mỹ dùng chung. Icon SVG ở `assets/icons.svg`, không cần thư viện bên ngoài.

## Bổ sung MySQL ngày 26/10/2026

Luồng hiện tại: Servlet → Controller → Service → DAO → JDBC/MySQL. `Database` xử lý cấu hình bí mật, JNDI pool, session UTC và transaction. `JdbcMap<V>` tái sử dụng model aggregate hiện có; `ConversationDao` truy vấn hội thoại bằng SQL có kiểm tra chủ sở hữu. `PersistenceFilter` giữ transaction cho POST và chỉ chuyển response thành công ra trình duyệt sau commit. Các lệnh gọi AI được thực hiện ngoài transaction chờ mạng. View mới AI Tutor không chứa SQL.

Đọc `DATABASE.md` để xem thiết kế quan hệ/JSON, schema, migration và cách chạy. Các ghi chú bộ nhớ trong báo cáo ngày 23–24/10 là trạng thái của phiên bản trước khi tích hợp MySQL.
