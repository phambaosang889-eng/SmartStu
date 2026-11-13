<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="author.*,com.smartstudy.service.AIService" %>
<%@ include file="start.jspf" %>
<%
    var accounts = (java.util.List<UserStore.Account>)request.getAttribute("systemAccounts");
    var systemAI = (AIService)request.getAttribute("systemAI");
%>
<section class="page-heading">
    <div>
        <span class="eyebrow">QUẢN TRỊ HỆ THỐNG</span>
        <h1>Tổng quan hệ thống</h1>
        <p>Quản lý tài khoản, kết nối AI và nhật ký hoạt động.</p>
    </div>
</section>
<div class="resource-grid">
    <% for (Role role : Role.values()) { %>
        <article class="resource-card">
            <h2><%=role==Role.ADMIN?"Quản trị viên":role==Role.TEACHER?"Giáo viên":"Học sinh"%></h2>
            <div class="card-metric">
                <strong><%=accounts.stream().filter(account -> account.user().getRole()==role).count()%></strong>
            </div>
            <a class="button secondary" href="<%=base%>/users">Quản lý tài khoản</a>
        </article>
    <% } %>
    <article class="resource-card">
        <h2>Tài khoản bị khóa</h2>
        <div class="card-metric">
            <strong><%=accounts.stream().filter(UserStore.Account::locked).count()%></strong>
        </div>
        <p>Trạng thái truy cập hiện tại trong hệ thống.</p>
    </article>
    <article class="resource-card">
        <h2>Kết nối AI</h2>
        <p><%=ViewSupport.h(systemAI.providerLabel())%></p>
        <p><%=systemAI.available()?"Sẵn sàng nhận yêu cầu":"Chưa cấu hình hoặc đang tạm dừng"%></p>
        <a class="button secondary" href="<%=base%>/ai-settings">Cấu hình AI</a>
    </article>
    <article class="resource-card">
        <h2>Nhật ký hệ thống</h2>
        <p>Kiểm tra đăng nhập, thao tác quản trị và thay đổi cấu hình.</p>
        <a class="button secondary" href="<%=base%>/audit">Xem nhật ký</a>
    </article>
</div>
<%@ include file="end.jspf" %>
