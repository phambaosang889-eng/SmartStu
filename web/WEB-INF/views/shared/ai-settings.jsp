<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.model.view.AIStatus" %>
<%@ include file="start.jspf" %>
<%
    AIStatus ai = (AIStatus) request.getAttribute("aiStatus");
    Object provider = request.getAttribute("aiProviderType");
    boolean admin = viewer.getRole() == Role.ADMIN;
    String connectionState = !ai.configured() ? "Chưa kết nối dịch vụ AI"
            : ai.enabled() ? "Đã cấu hình kết nối" : "Dịch vụ đang tạm dừng";
%>
<section class="page-heading">
    <div>
        <span class="eyebrow">DỊCH VỤ AI</span>
        <h1><%=admin ? "Cấu hình AI" : "Trạng thái dịch vụ AI"%></h1>
        <p>Theo dõi kết nối, yêu cầu và giới hạn sử dụng.</p>
    </div>
</section>
<% if (request.getAttribute("aiError") != null) { %>
    <div class="notice error" role="alert"><%=ViewSupport.h(request.getAttribute("aiError"))%></div>
<% } %>
<div class="notice">
    <b><%=connectionState%></b> <span><%=ViewSupport.h(request.getAttribute("aiProvider"))%></span> <span><%=ViewSupport.h(ai.lastState())%></span>
</div>
<div class="stat-grid">
    <div class="stat-card">
        <span>Yêu cầu đã phản hồi</span><strong><%=ai.successes()%></strong><span>Trong phiên chạy hiện tại</span>
    </div>
    <div class="stat-card">
        <span>Lỗi đã ghi nhận</span><strong><%=ai.failures()%></strong><span>Trong phiên chạy hiện tại</span>
    </div>
    <div class="stat-card">
        <span>Giới hạn mỗi 5 phút</span><strong><%=ai.limit()%></strong><span>Theo từng người dùng</span>
    </div>
    <div class="stat-card">
        <span>Thời gian chờ tối đa</span><strong>25 giây</strong><span>Cho mỗi yêu cầu AI</span>
    </div>
</div>
<% if (admin) { %>
    <form class="surface editor-form" method="post" action="<%=base%>/ai-settings">
        <h2>Model và chính sách sử dụng</h2>
        <input type="hidden" name="csrf" value="<%=ViewSupport.csrf(request)%>">
        <label>
            Nhà cung cấp
            <select name="provider">
                <option value="gemini" <%="gemini".equals(provider) ? "selected" : ""%>>Google Gemini</option>
                <option value="gateway" <%="gateway".equals(provider) ? "selected" : ""%>>AI Gateway</option>
            </select>
        </label>
        <label>
            Model Gemini
            <input
                name="model"
                value="<%=ViewSupport.h(request.getAttribute("aiModel"))%>"
                pattern="gemini-[a-zA-Z0-9._\x2d]{1,100}"
                maxlength="107"
                required>
        </label>
        <label class="check-line">
            <input type="checkbox" name="enabled" <%=ai.enabled() ? "checked" : ""%>>
            Cho phép sử dụng dịch vụ
        </label>
        <label>
            Số yêu cầu tối đa mỗi 5 phút
            <input type="number" name="limit" value="<%=ai.limit()%>" min="1" max="30" required>
        </label>
        <button class="button primary" type="submit">Lưu chính sách</button>
    </form>
    <form class="surface editor-form" method="post" action="<%=base%>/ai-settings" autocomplete="off">
        <h2>Cấu hình kết nối</h2>
        <input type="hidden" name="csrf" value="<%=ViewSupport.csrf(request)%>">
        <input type="hidden" name="action" value="update-key">
        <label>
            Nhà cung cấp của API key
            <select name="keyProvider">
                <option value="gemini" <%="gemini".equals(provider) ? "selected" : ""%>>Google Gemini</option>
                <option value="gateway" <%="gateway".equals(provider) ? "selected" : ""%>>AI Gateway</option>
            </select>
        </label>
        <label>
            API key mới
            <input
                type="password"
                name="apiKey"
                autocomplete="new-password"
                maxlength="512"
                spellcheck="false"
                autocapitalize="none">
        </label>
        <p class="field-help">Để trống để giữ khóa hiện tại. Khóa mới áp dụng từ yêu cầu tiếp theo.</p>
        <button class="button primary" type="submit">Lưu API key</button>
    </form>
<% } else { %>
    <section class="surface">
        <h2>Khi dịch vụ chưa khả dụng</h2>
        <p>Bạn có thể dùng câu hỏi mẫu, soạn câu hỏi mới và chấm tự luận theo rubric.</p>
        <a class="button primary" href="<%=base%>/ai-questions">Chuẩn bị câu hỏi</a>
    </section>
<% } %>
<%@ include file="end.jspf" %>
