<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>SmartStudy • Quản lý Quiz</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    </head>
    <body class="app-page">
        <aside class="sidebar">
            <a class="brand" href="${pageContext.request.contextPath}/teacher/dashboard"><span
                    class="hex-logo"><b>S</b></span><span>Smart<span>Study</span></span></a>
            <div class="side-label">TỔNG QUAN</div>
            <nav class="side-nav">
                <a href="${pageContext.request.contextPath}/teacher/dashboard">⌂ <span>Dashboard</span></a>
            </nav>
            <div class="side-label">GIẢNG DẠY</div>
            <nav class="side-nav">
                <a href="${pageContext.request.contextPath}/teacher/courses">▣ <span>Khóa học của tôi</span></a>
                <a href="${pageContext.request.contextPath}/teacher/lesson">▶ <span>Quản lý bài học</span></a>
                <a class="active" href="${pageContext.request.contextPath}/teacher/quiz">? <span>Quản lý Quiz</span></a>
                <a href="${pageContext.request.contextPath}/teacher/flashcards">◇ <span>Quản lý Flashcard</span></a>
            </nav>
            <div class="side-label">HỌC SINH & THÔNG BÁO</div>
            <nav class="side-nav">
                <a href="${pageContext.request.contextPath}/teacher/students">👥 <span>Học sinh</span></a>
                <a href="${pageContext.request.contextPath}/teacher/notifications">🔔 <span>Tạo thông báo</span></a>
            </nav>
            <div class="side-label">TÀI KHOẢN</div>
            <nav class="side-nav"><a
                    href="${pageContext.request.contextPath}/logout">⇥ <span>Đăng xuất</span></a></nav>
        </aside>
        <main class="main-content">
            <header class="app-header">
                <div><span class="eyebrow">TEACHER • QUIZ</span><h2>Quản lý Quiz</h2></div>
                <div class="header-actions">
                    <button class="icon-btn theme-toggle" type="button" aria-label="Đổi giao diện"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path d="M20.9 13A9 9 0 0 1 11 3.1 9 9 0 1 0 20.9 13Z"/></svg></button>
                    <div class="avatar">T</div>
                </div>
            </header>
            <section class="management-page">
                <div class="management-header"><div><span class="eyebrow">QUIZ MANAGEMENT</span><h1>Quản lý Quiz</h1><p>Tạo bộ câu hỏi mẫu và đưa câu đã rà soát vào ngân hàng.</p></div><a class="btn btn-primary" href="${pageContext.request.contextPath}/teacher/ai-questions">✦ AI tạo câu hỏi</a></div>
                <div class="management-toolbar"><input class="management-search" type="search" placeholder="Tìm Quiz..." aria-label="Tìm Quiz"><select class="management-select"><option>Tất cả trạng thái</option><option>Đang hoạt động</option><option>Bản nháp</option></select></div>
                <div class="content-list">
                    <div class="content-row"><span class="content-icon orange">?</span><div class="content-main"><b>Quiz Định luật II Newton</b><span>Bài mẫu giao diện · số lượt làm chưa kết nối dữ liệu</span></div><span class="status draft">Dữ liệu mẫu</span><a class="btn btn-outline" href="${pageContext.request.contextPath}/teacher/question-bank">Ngân hàng câu hỏi</a></div>
                    <div class="content-row"><span class="content-icon purple">?</span><div class="content-main"><b>Quiz Lực và chuyển động</b><span>Bài mẫu giao diện · số lượt làm chưa kết nối dữ liệu</span></div><span class="status draft">Dữ liệu mẫu</span><a class="btn btn-outline" href="${pageContext.request.contextPath}/teacher/ai-questions">Tạo câu hỏi</a></div>
                    <div class="content-row"><span class="content-icon blue">?</span><div class="content-main"><b>Ôn tập chương Cơ học</b><span>Quiz mẫu chưa được phát hành</span></div><span class="status draft">Bản nháp</span><a class="btn btn-outline" href="${pageContext.request.contextPath}/teacher/ai-questions">Tạo câu hỏi</a></div>
                </div>
            </section>
        </main>
        <script src="${pageContext.request.contextPath}/assets/js/app.js?v=20261030"></script>
    <script src="${pageContext.request.contextPath}/assets/js/theme.js?v=20261030" charset="UTF-8"></script></body>
</html>
