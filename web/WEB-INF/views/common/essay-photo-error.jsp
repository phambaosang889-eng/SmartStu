<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.web.ViewSupport" %>
    <!doctype html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Ảnh bài tự luận · SmartStu</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/workspace.css">
    </head>

    <body>
        <main class="error-layout surface">
            <h1>Chưa xử lý được ảnh bài làm</h1>
            <p role="alert">
                <%=ViewSupport.h(request.getAttribute("photoError"))%>
            </p>
            <p>Bài và ảnh đã lưu trước đó vẫn được giữ.</p><a class="button primary"
                href="<%=ViewSupport.h(request.getAttribute(" photoBack"))%>">Quay về bài tự luận</a>
        </main>
    </body>

    </html>