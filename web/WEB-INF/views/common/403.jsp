<%@ page contentType="text/html;charset=UTF-8" %>
    <!doctype html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>403 • SmartStudy</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    </head>

    <body class="auth-page">
        <div class="auth-form" style="max-width:560px;margin:10vh auto;text-align:center"><span class="eyebrow">ACCESS
                DENIED</span>
            <h2>403 - Không có quyền truy cập</h2>
            <p>Tài khoản hiện tại không có quyền mở trang này. Hãy dùng đúng vai trò hoặc đăng nhập lại.</p>
            <div style="display:flex;gap:10px;justify-content:center"><a class="btn btn-primary"
                    href="${pageContext.request.contextPath}/home">Trang chủ</a><a class="btn btn-soft"
                    href="${pageContext.request.contextPath}/logout">Đăng nhập lại</a></div>
        </div>
    </body>

    </html>