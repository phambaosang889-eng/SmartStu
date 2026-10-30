<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.service.CatalogService" %>
    <%@ include file="start.jspf" %>
        <section class="page-heading">
            <div><span class="eyebrow">THƯ VIỆN GIẢNG DẠY</span>
                <h1>Quản lý nội dung</h1>
                <p>Một nơi để biên soạn, tổ chức và xuất bản học liệu Vật lý 10.</p>
            </div><button class="button primary" type="button" data-open="create-content">＋ Thêm nội dung</button>
        </section>
        <div class="notice"><span class="notice-icon">i</span>
            <p>Nội dung được dùng chung giữa quản trị viên, giáo viên và học sinh. Chỉ bản đã xuất bản mới xuất hiện
                trong thư viện học tập.</p>
        </div>
        <div class="resource-grid">
            <% String[][] cats={{"lesson","01","Bài học","Lý thuyết, ví dụ và câu hỏi luyện tập theo từng chủ đề."},{"flashcards","02","Flashcard","Thẻ ghi nhớ ngắn gọn, câu hỏi ở mặt trước và lời giải ở mặt sau."},{"courses","03","Khóa học","Tổ chức lộ trình và mục tiêu học tập cho lớp học."}}; for(String[]
                cat:cats){int total=CatalogService.getInstance().list(cat[0],viewer).size();int
                published=CatalogService.getInstance().published(cat[0]).size(); %>
                <article class="resource-card">
                    <div class="card-top"><span class="resource-icon">
                            <%=cat[1]%>
                        </span><span class="badge">
                            <%=published%> đã xuất bản
                        </span></div>
                    <h2>
                        <%=cat[2]%>
                    </h2>
                    <p>
                        <%=cat[3]%>
                    </p>
                    <div class="card-metric"><strong>
                            <%=total%>
                        </strong><span>nội dung trong thư viện</span></div>
                    <div class="actions"><a class="button secondary" href="<%=base%>/<%=cat[0]%>">Mở thư viện →</a><a
                            class="text-link" href="<%=base%>/<%=cat[0]%>?mode=edit">Thêm mới</a></div>
                </article>
                <% } %>
        </div>
        <section class="surface callout">
            <div><span class="eyebrow">KIỂM TRA & LUYỆN TẬP</span>
                <h2>Quiz và ngân hàng câu hỏi</h2>
                <p>Quản lý bài kiểm tra, câu hỏi và đáp án ở không gian quiz.</p>
            </div><a class="button secondary" href="<%=base%>/quiz">Mở Quiz →</a>
        </section>
        <dialog id="create-content" aria-labelledby="create-title">
            <div class="dialog-heading">
                <h2 id="create-title">Bạn muốn thêm nội dung gì?</h2><button type="button" class="icon-button"
                    data-close aria-label="Đóng">×</button>
            </div>
            <p>Chọn loại học liệu để mở biểu mẫu biên soạn.</p>
            <div class="choice-list">
                <% for(String[] cat:cats){%><a href="<%=base%>/<%=cat[0]%>?mode=edit"><b>
                            <%=cat[2]%>
                        </b><span>
                            <%=cat[3]%> →
                        </span></a>
                    <%}%>
            </div>
        </dialog>
        <%@ include file="end.jspf" %>