<%@ page contentType="text/html;charset=UTF-8"
    import="com.smartstudy.persistence.Database,com.smartstudy.web.ViewSupport" %>
    <!doctype html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1.0">
        <title>SmartStudy • Đăng nhập</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
        <style>
            .role-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 10px;
                margin: 10px 0 18px
            }

            .role-option {
                position: relative
            }

            .role-option input {
                position: absolute;
                opacity: 0;
                pointer-events: none
            }

            .role-option span {
                display: block;
                text-align: center;
                padding: 13px 8px;
                border: 1px solid var(--line);
                border-radius: 12px;
                background: #fff;
                font-weight: 700;
                font-size: 13px;
                cursor: pointer;
                transition: .2s
            }

            .role-option input:checked+span {
                border-color: var(--primary);
                background: var(--soft);
                color: var(--primary);
                box-shadow: 0 0 0 2px rgba(14, 165, 233, .12)
            }

            .demo-list {
                display: grid;
                gap: 7px;
                margin-top: 10px;
                font-size: 12px;
                color: var(--muted)
            }

            .demo-list code {
                font-family: inherit;
                color: var(--text);
                font-weight: 600
            }
        </style>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/mobile.css?v=20261028">
    </head>

    <body class="auth-page">
        <div class="auth-shell">
            <div class="auth-art">
                <a class="brand light-brand" href="${pageContext.request.contextPath}/home">
                    <span class="hex-logo"><b>S</b></span><span>Smart<span>Study</span></span>
                </a>
                <div class="auth-art-copy">
                    <div class="eyebrow">CÙNG HỌC VẬT LÝ 10</div>
                    <h1>Biến công thức<br>thành <span>hiểu biết.</span></h1>
                    <p>Chọn đúng vai trò để vào không gian làm việc của bạn.</p>
                    <div class="formula-stack"><span>v = v₀ + at</span><span>F = ma</span><span>W = Fs cos α</span>
                    </div>
                </div>
            </div>
            <div class="auth-form">
                <div class="auth-head">
                    <span class="eyebrow">CHÀO MỪNG TRỞ LẠI</span>
                    <h2>Đăng nhập</h2>
                    <p>Chọn vai trò</p>
                </div>
                <%if(request.getAttribute("authNotice")!=null){%>
                    <div class="alert" role="status">
                        <%=ViewSupport.h(request.getAttribute("authNotice"))%>
                    </div>
                    <%}%>
                        <% if ("1".equals(request.getParameter("error"))) { %>
                            <div class="alert error">Thông tin đăng nhập hoặc vai trò không đúng. Vui lòng kiểm tra lại.
                            </div>
                            <% } else if ("role".equals(request.getParameter("error"))) { %>
                                <div class="alert error">Vui lòng chọn Học sinh, Giáo viên hoặc Quản trị.</div>
                                <% } else if ("session".equals(request.getParameter("error"))) { %>
                                    <div class="alert error" role="alert">Phiên đăng nhập không còn hiệu lực hoặc tài
                                        khoản đã đổi trạng thái. Vui lòng đăng nhập lại; liên hệ quản trị viên nếu tài
                                        khoản bị khóa.</div>
                                    <% } else if ("1".equals(request.getParameter("logout"))) { %>
                                        <div class="alert">Bạn đã đăng xuất thành công.</div>
                                        <% } %>

                                            <form method="post" action="${pageContext.request.contextPath}/login">
                                                <input type="hidden" name="csrf" value="<%=ViewSupport.csrf(request)%>">
                                                <label>Vai trò</label>
                                                <div class="role-grid">
                                                    <label class="role-option"><input type="radio" name="role"
                                                            value="STUDENT" checked><span>Học sinh</span></label>
                                                    <label class="role-option"><input type="radio" name="role"
                                                            value="TEACHER"><span>Giáo viên</span></label>
                                                    <label class="role-option"><input type="radio" name="role"
                                                            value="ADMIN"><span>Quản trị</span></label>
                                                </div>
                                                <label>Email<input id="email" type="email" name="email"
                                                        value="<%=Database.memory()?" student@smartstudy.vn":""%>"
                                                    required></label>
                                                <label>Mật khẩu<div class="password-wrap"><input id="password"
                                                            type="password" name="password"
                                                            value="<%=Database.memory()?" 123456":""%>" required><button
                                                            type="button" class="show-pass"
                                                            onclick="togglePassword()">◉</button></div></label>
                                                <div class="form-row"><span class="muted">Phiên đăng nhập trên thiết bị
                                                        này</span><a
                                                        href="${pageContext.request.contextPath}/home">Trang chủ</a>
                                                </div>
                                                <button class="btn btn-primary full" type="submit">Đăng nhập →</button>
                                            </form>

                                            <%if(Database.memory()){%>
                                                <details class="demo-box">
                                                    <summary>Tài khoản trải nghiệm</summary>

                                                    <div class="demo-list">
                                                        <span>Học sinh: <code>student@smartstudy.vn</code> /
                                                            <code>123456</code></span>
                                                        <span>Giáo viên: <code>teacher@smartstudy.vn</code> /
                                                            <code>123456</code></span>
                                                        <span>Quản trị: <code>admin@smartstudy.vn</code> /
                                                            <code>123456</code></span>
                                                    </div>
                                                </details>
                                                <%}%>
                                                    <p class="auth-bottom">Chưa có tài khoản? <a
                                                            href="${pageContext.request.contextPath}/register">Tạo tài
                                                            khoản học sinh</a></p>
            </div>
        </div>
        <script>
            function togglePassword() { const p = document.getElementById('password'); p.type = p.type === 'password' ? 'text' : 'password'; }
            const roleEmails = { STUDENT: 'student@smartstudy.vn', TEACHER: 'teacher@smartstudy.vn', ADMIN: 'admin@smartstudy.vn' };
<%if (Database.memory()) {%> document.querySelectorAll('input[name="role"]').forEach(r => r.addEventListener('change', () => document.getElementById('email').value = roleEmails[r.value]));<%}%>
        </script>
    </body>

    </html>