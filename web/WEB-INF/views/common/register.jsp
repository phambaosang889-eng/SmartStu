<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.web.ViewSupport" %>
    <!doctype html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Tạo tài khoản · SmartStu</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/registration.css?v=20261026">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/mobile.css?v=20261028">
    </head>

    <body class="auth-page">
        <div class="auth-shell">
            <section class="auth-art">
                <a class="brand light-brand" href="${pageContext.request.contextPath}/home"><span
                        class="hex-logo"><b>S</b></span><span>Smart<span>Study</span></span></a>
                <div class="auth-art-copy">
                    <div class="eyebrow">BẮT ĐẦU HÀNH TRÌNH HỌC TẬP</div>
                    <h1>Hiểu kiến thức.<br><span>Tự tin vận dụng.</span></h1>
                    <p>Tạo tài khoản để tham gia khóa học, luyện tập và lưu tiến độ Vật lý 10 của riêng bạn.</p>
                    <div class="formula-stack"><span>F = ma</span><span>v = v₀ + at</span><span>W = Fs cos α</span>
                    </div>
                </div>
            </section>
            <main class="auth-form">
                <div class="auth-head"><span class="eyebrow">KHÔNG GIAN CỦA BẠN</span>
                    <h1>Tạo tài khoản</h1>
                    <p>Dành cho học sinh. Tài khoản giáo viên được quản trị viên cấp.</p>
                </div>
                <%if(request.getAttribute("registrationError")!=null){%>
                    <div class="alert error" role="alert">
                        <%=ViewSupport.h(request.getAttribute("registrationError"))%>
                    </div>
                    <%}%>
                        <form method="post" action="${pageContext.request.contextPath}/register" id="registration-form">
                            <input type="hidden" name="csrf" value="<%=ViewSupport.csrf(request)%>">
                            <label for="register-name">Họ và tên<input id="register-name" type="text" name="name"
                                    autocomplete="name" maxlength="100" value="<%=ViewSupport.h(request.getAttribute(" registrationName"))%>" required></label>
                            <label for="register-email">Email<input id="register-email" name="email" type="email"
                                    autocomplete="email" maxlength="150" value="<%=ViewSupport.h(request.getAttribute(" registrationEmail"))%>" required></label>
                            <label for="register-password">Mật khẩu<input id="register-password" name="password"
                                    type="password" autocomplete="new-password" minlength="8" maxlength="200"
                                    aria-describedby="password-help" required></label>
                            <small id="password-help" class="registration-help">Từ 8–200 ký tự. Nên kết hợp chữ, số và
                                ký hiệu.</small>
                            <label for="register-confirm">Nhập lại mật khẩu<input id="register-confirm"
                                    name="confirmPassword" type="password" autocomplete="new-password" minlength="8"
                                    maxlength="200" required></label>
                            <label class="registration-show"><input type="checkbox" id="show-registration-password">
                                Hiện mật khẩu</label>
                            <button class="btn btn-primary full" type="submit">Tạo tài khoản →</button>
                            <p id="registration-status" role="status" hidden>Đang tạo tài khoản, vui lòng chờ…</p>
                        </form>
                        <p class="auth-bottom">Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng
                                nhập</a></p>
                        <p class="registration-help">Không cần thông tin nhạy cảm để bắt đầu học. Bạn có thể cập nhật
                            tên và mật khẩu trong Hồ sơ cá nhân.</p>
            </main>
        </div>
        <script src="${pageContext.request.contextPath}/assets/js/registration.js?v=20261026" defer></script>
    </body>

    </html>