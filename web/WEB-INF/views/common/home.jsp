<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.service.*,com.smartstudy.web.ViewSupport" %>
    <!doctype html>
    <html lang="vi">

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width,initial-scale=1.0">
        <title>SmartStudy • Vật lý 10</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/mobile.css?v=20261028"></head>

    <body data-theme="light">
        <header class="topbar public-top"><a class="brand" href="${pageContext.request.contextPath}/home"><span
                    class="hex-logo"><b>S</b></span><span>Smart<span>Study</span></span></a>
            <nav><a class="active" href="${pageContext.request.contextPath}/home">Trang chủ</a><a
                    href="${pageContext.request.contextPath}/login">Khóa học</a><a
                    href="${pageContext.request.contextPath}/login">Tiến độ</a></nav>
            <div class="top-actions"><button class="icon-btn theme-toggle" title="Đổi giao diện"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M20.9 13A9 9 0 0 1 11 3.1 9 9 0 1 0 20.9 13Z"/></svg></button><a
                    class="btn btn-outline" href="${pageContext.request.contextPath}/login">Đăng nhập</a></div>
        </header>
        <main>
            <section class="hero container">
                <div class="hero-copy">
                    <div class="eyebrow">SMART LEARNING • VẬT LÝ 10</div>
                    <h1>Học Vật lý 10<br><span>theo cách thông minh.</span></h1>
                    <p>Nắm chắc công thức, hiểu bản chất và luyện tập qua bài học trực quan, quiz và flashcard. Tất cả
                        trong một không gian học tập gọn gàng.</p>
                    <div class="hero-actions"><a class="btn btn-primary btn-lg"
                            href="${pageContext.request.contextPath}/login">Bắt đầu học →</a><a
                            class="btn btn-soft btn-lg" href="${pageContext.request.contextPath}/login">Khám phá khóa
                            học</a></div>
                    <div class="mini-stats">
                        <div><b><%=CatalogService.getInstance().published("lesson").stream().map(e->e.topic()).distinct().count()%></b><span>Chủ đề</span></div>
                        <div><b><%=CatalogService.getInstance().published("lesson").size()%></b><span>Bài học</span></div>
                        <div><b><%=QuizService.getInstance().questions().size()%></b><span>Câu hỏi</span></div>
                    </div>
                </div>
                <div class="hero-visual">
                    <div class="orbit-card">
                        <div class="orbit-ring"></div>
                        <div class="physics-core">F = ma</div><span class="float-chip chip-a">⚡ Động lực học</span><span
                            class="float-chip chip-b">📐 Công thức</span><span class="float-chip chip-c">✓ Luyện tập Quiz</span>
                    </div>
                </div>
            </section>
            <section class="container section">
                <div class="section-heading">
                    <div><span class="eyebrow">KHÓA HỌC NỔI BẬT</span>
                        <h2>Lộ trình Vật lý 10</h2>
                    </div><a href="${pageContext.request.contextPath}/login">Xem tất cả →</a>
                </div>
                <div class="course-grid">
                <%for(var course:CatalogService.getInstance().published("courses")){%>
                    <article class="course-card"><div class="course-cover cover-1"><span>F = ma</span><strong><%=ViewSupport.h(course.topic())%></strong><small>Học theo chương · Luyện theo chủ đề</small></div><div class="course-body"><div class="tag">KHÓA HỌC ĐÃ XUẤT BẢN</div><h3><%=ViewSupport.h(course.title())%></h3><p><%=CourseService.getInstance().chapters(course.id()).size()%> chương · <%=CourseService.getInstance().lessons(course.id()).size()%> bài học</p><a class="btn btn-primary" href="${pageContext.request.contextPath}/login">Đăng nhập để tham gia →</a></div></article>
                <%}if(CatalogService.getInstance().published("courses").isEmpty()){%><p>Giáo viên đang chuẩn bị khóa học. Hãy quay lại sau để tham gia.</p><%}%>
                </div>            </section>
            <section class="container section feature-strip">
                <div><span class="feature-icon">▶</span><b>Bài học ngắn</b><small>Học từng khái niệm trong 10–15
                        phút.</small></div>
                <div><span class="feature-icon">?</span><b>Quiz thông minh</b><small>Kiểm tra ngay sau mỗi chủ
                        đề.</small></div>
                <div><span class="feature-icon">✦</span><b>Flashcard</b><small>Ghi nhớ công thức và định nghĩa.</small>
                </div>
                <div><span class="feature-icon">↗</span><b>Theo dõi tiến độ</b><small>Biết mình đang ở đâu mỗi
                        ngày.</small></div>
            </section>
        </main>
        <footer class="footer">© 2026 SmartStudy • Không gian học tập • Vật lý 10</footer>
        <script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261030"></script>
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=20261030" charset="UTF-8"></script></body>

    </html>