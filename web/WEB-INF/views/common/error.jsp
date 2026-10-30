<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true"
    import="author.User,com.smartstudy.web.ViewSupport" %>
    <%int code=response.getStatus();if(code<400)code=500;User person=(User)session.getAttribute("user");%>
        <!doctype html>
        <html lang="vi">

        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <title>
                <%=code%> · SmartStu
            </title>
            <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/workspace.css">
        </head>

        <body>
            <main class="error-layout">
                <section class="surface"><span class="eyebrow">SMARTSTU · <%=code%></span>
                    <h1>
                        <%=code==403?"Bạn không có quyền mở nội dung này":code==404?"Không tìm thấy nội dung":code==400?"Yêu cầu chưa hợp lệ":"Không thể xử lý yêu cầu lúc này"%>
                    </h1>
                    <p>
                        <%=code==403?"Hãy dùng đúng vai trò và khóa học được phân công. Nếu phiên biểu mẫu hết hạn, mở lại trang rồi thử lại.":code==404?"Nội dung có thể chưa xuất bản, đã lưu trữ hoặc không thuộc khóa học của bạn.":code==400?"Mở lại biểu mẫu và kiểm tra dữ liệu trước khi gửi.":"Yêu cầu chưa hoàn tất. Quay về tổng quan và thử lại; liên hệ quản trị viên nếu lỗi tiếp tục."%>
                    </p>
                    <div class="actions"><a class="button primary" href="<%=request.getContextPath()%><%=person==null?" /login":"/"+ViewSupport.role(person)+"/dashboard"%>">Về <%=person==null?"đăng nhập":"tổng quan"%></a><a class="button secondary"
                            href="${pageContext.request.contextPath}/home">Trang chủ</a></div>
                </section>
            </main>
        </body>

        </html>