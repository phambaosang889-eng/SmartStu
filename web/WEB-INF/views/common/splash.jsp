<%@ page contentType="text/html;charset=UTF-8" %>
  <!doctype html>
  <html lang="vi">

  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <title>SmartStudy • Loading</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
  </head>

  <body class="splash-page">
    <div class="splash-bg"><span></span><span></span><span></span></div>
    <main class="splash-card">
      <div class="hex-logo large"><span>S</span></div>
      <div class="brand-title">Smart<span>Study</span></div>
      <p>Học Vật lý 10 thông minh hơn • Tập trung hơn • Tiến bộ mỗi ngày</p>
      <div class="loading-line"><i></i></div>
      <small>Đang khởi tạo không gian học tập...</small>
    </main>
    <script>setTimeout(() => location.href = '${pageContext.request.contextPath}/home', 1800);</script>
  </body>

  </html>