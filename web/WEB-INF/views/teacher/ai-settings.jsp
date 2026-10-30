<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/views/shared/start.jspf" %>
<section class="management-page"><h1>Tích hợp dịch vụ AI</h1><div class="alert">Chưa cấu hình dịch vụ AI. Ứng dụng hiện dùng dữ liệu mẫu có nhãn rõ ràng; không có API key trong mã nguồn.</div><div class="panel"><h3>Cấu hình khi triển khai</h3><p>Đặt khóa API trong biến môi trường phía máy chủ, sau đó bổ sung AI service và kiểm tra phản hồi JSON trước khi dùng. Không nhập khóa ở trình duyệt hoặc JSP.</p><p>Hiện các luồng mẫu không gửi dữ liệu học sinh ra dịch vụ bên ngoài.</p><a href="${pageContext.request.contextPath}/teacher/ai-questions">Quay lại tạo câu hỏi →</a></div></section>

<%@ include file="/WEB-INF/views/shared/end.jspf" %>