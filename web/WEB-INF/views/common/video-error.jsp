<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.web.ViewSupport" %>
    <!doctype html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Chưa cập nhật được video · SmartStu</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/workspace.css">
    </head>

    <body>
        <main class="surface" style="max-width:720px;margin:10vh auto;padding:32px"><span class="eyebrow">VIDEO BÀI
                GIẢNG</span>
            <h1>Chưa cập nhật được video</h1>
            <p role="alert">
                <%=ViewSupport.h(request.getAttribute("videoError"))%>
            </p><a class="button primary" href="${pageContext.request.contextPath}/teacher/lesson">Về danh sách bài
                học</a>
            <p>Thông tin bài học vẫn được giữ. Mở Chỉnh sửa để chọn lại video.</p>
        </main>
    </body>

    </html>