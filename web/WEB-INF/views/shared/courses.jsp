<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"
    import="com.smartstudy.model.CatalogEntry,com.smartstudy.service.CourseService,com.smartstudy.service.CourseService.*"
    %>
    <%@ include file="start.jspf" %>
        <%boolean
            teacher=viewer.getRole()==Role.TEACHER,admin=viewer.getRole()==Role.ADMIN,student=viewer.getRole()==Role.STUDENT;CatalogEntry
            course=(CatalogEntry)request.getAttribute("course");boolean
            edit=teacher&&("edit".equals(request.getParameter("mode"))||"save-course".equals(request.getParameter("action")));boolean
            err=request.getAttribute("courseError")!=null;CourseService cs=CourseService.getInstance();%>
            <section class="page-heading">
                <div><span class="eyebrow">
                        <%=admin?"KIỂM DUYỆT & GIÁM SÁT":"LỘ TRÌNH VẬT LÝ 10"%>
                    </span>
                    <h1>
                        <%=edit?course==null?"Tạo khóa học":"Chỉnh sửa khóa học":course==null?"Khóa học":ViewSupport.h(course.title())%>
                    </h1>
                    <p>
                        <%=admin?"Xem nội dung, kiểm duyệt xuất bản và theo dõi đăng ký. Việc biên soạn và chấm điểm thuộc giáo viên phụ trách.":"Từ kiến thức nền tảng đến vận dụng qua bài học và luyện tập."%>
                    </p>
                </div>
                <div class="actions">
                    <%if(course!=null||edit){%><a class="button secondary" href="<%=base%>/courses">← Danh sách</a>
                        <%}if(teacher&&!edit){%><a class="button primary" href="<%=base%>/courses?mode=edit">＋ Tạo khóa
                                học</a>
                            <%}%>
                </div>
            </section>
            <%if(err){%>
                <div class="notice error" role="alert">
                    <%=ViewSupport.h(request.getAttribute("courseError"))%>
                </div>
                <%}%>
                    <%if(edit){%>
                        <form class="surface editor-form" method="post" action="<%=base%>/courses"><input type="hidden"
                                name="csrf" value="<%=ViewSupport.csrf(request)%>"><input type="hidden" name="action"
                                value="save-course"><input type="hidden" name="id"
                                value="<%=course==null?"":course.id()%>"><input type="hidden" name="version"
                                value="<%=course==null?0:course.version()%>"><label>Tên khóa học<input name="title"
                                    maxlength="150" required value="<%=ViewSupport.h(err?request.getParameter(" title"):course==null?"":course.title())%>"></label><label>Danh mục / chủ đề<input
                                    list="course-topics" name="topic" maxlength="80" required
                                    value="<%=ViewSupport.h(err?request.getParameter(" topic"):course==null?"Vật lý 10":course.topic())%>"></label><datalist id="course-topics">
                                <%for(var
                                    topic:com.smartstudy.service.CatalogService.getInstance().published("topics")){%>
                                    <option value="<%=ViewSupport.h(topic.title())%>">
                                        <%}%>
                            </datalist><label>Ảnh đại diện (đường dẫn HTTPS, không bắt buộc)<input type="url"
                                    name="cover" maxlength="1000" value="<%=ViewSupport.h(err?request.getParameter(" cover"):course==null?"":cs.cover(course.id()))%>"></label><label>Mô tả, mục tiêu và
                                hướng dẫn học<textarea name="body" rows="8" maxlength="12000"
                                    required><%=ViewSupport.h(err?request.getParameter("body"):course==null?"":course.body())%></textarea></label><label>Trạng
                                thái<select name="status">
                                    <option value="draft">Lưu bản nháp / ẩn để chỉnh sửa</option>
                                    <option value="pending">Gửi quản trị viên duyệt xuất bản</option>
                                    <option value="archived">Lưu trữ (có thể mở lại)</option>
                                </select></label>
                            <p class="muted">Sau khi lưu nội dung mới, khóa học cần được duyệt lại trước khi hiển thị
                                cho học sinh. Thêm chương và bài giảng ở trang chi tiết sau khi lưu.</p><button
                                class="button primary">Lưu khóa học</button>
                        </form>
                        <%}else if(course!=null){boolean
                            enrolled=cs.enrolled(viewer.getEmail(),course.id());List<Chapter> chapters=(List<Chapter>
                                )request.getAttribute("chapters");List<CatalogEntry> lessons=(List<CatalogEntry>
                                        )request.getAttribute("courseLessons");%>
                                        <section class="surface"><span class="badge">
                                                <%=ViewSupport.status(course.status())%>
                                            </span>
                                            <h2>Giới thiệu khóa học</h2>
                                            <div class="prose">
                                                <%=ViewSupport.h(course.body())%>
                                            </div>
                                            <p>Giáo viên: <b>
                                                    <%=ViewSupport.h(author.UserStore.active(course.owner())==null?course.owner():author.UserStore.active(course.owner()).getName())%>
                                                </b> · Cập nhật: <%=ViewSupport.date(course.updatedAt())%>
                                            </p>
                                            <p>
                                                <%=chapters.size()%> chương · <%=lessons.size()%> bài đã xuất bản ·
                                                        <%=cs.enrollments(viewer).stream().filter(e->e.active()&&e.course().equals(course.id())).count()%>
                                                            lượt đăng ký trong phạm vi bạn được xem
                                            </p>
                                            <%if(student){%>
                                                <form method="post" action="<%=base%>/courses"
                                                    data-confirm="<%=enrolled?" Dừng tham gia khóa học? Tiến độ của bạn vẫn được giữ.":"Đăng ký tham gia khóa học này?"%>"><input
                                                        type="hidden" name="csrf"
                                                        value="<%=ViewSupport.csrf(request)%>"><input type="hidden"
                                                        name="action" value="<%=enrolled?" leave":"enroll"%>"><input
                                                        type="hidden" name="id" value="<%=course.id()%>">
                                                    <div class="actions"><button class="button <%=enrolled?" secondary":"primary"%>"><%=enrolled?"Dừng tham gia":"Đăng ký khóa học"%></button>
                                                        <%if(enrolled){%><a class="button primary"
                                                                href="<%=base%>/lesson?course=<%=course.id()%>">Tiếp tục
                                                                học →</a><span>
                                                                <%=Math.round(cs.progress(viewer.getEmail(),course.id()))%>
                                                                    % hoàn thành
                                                            </span>
                                                            <%}%>
                                                    </div>
                                                </form>
                                                <%}if(teacher){%>
                                                    <div class="actions"><a class="button primary"
                                                            href="<%=base%>/courses?mode=edit&id=<%=course.id()%>">Sửa
                                                            khóa học / trạng thái</a><a class="button secondary"
                                                            href="<%=base%>/lesson?mode=edit&course=<%=course.id()%>">＋
                                                            Thêm bài giảng</a><a class="button secondary"
                                                            href="<%=base%>/students">Học sinh của khóa học</a></div>
                                                    <%}%>
                                        </section>
                                        <section class="surface">
                                            <h2>Nội dung theo chương</h2>
                                            <%if(chapters.isEmpty()){%>
                                                <p>Chưa có chương. Giáo viên cần thêm chương trước khi thêm bài giảng.
                                                </p>
                                                <%}for(Chapter ch:chapters){%>
                                                    <details class="question-choice" open>
                                                        <summary>
                                                            <%=ch.order()%>. <%=ViewSupport.h(ch.title())%>
                                                        </summary>
                                                        <%List<CatalogEntry>
                                                            chapterLessons=lessons.stream().filter(l->cs.placement(l.id()).chapter().equals(ch.id())).toList();if(chapterLessons.isEmpty()){%>
                                                            <p>Chưa có bài giảng được xuất bản trong chương này.</p>
                                                            <%}for(CatalogEntry l:chapterLessons){%>
                                                                <div class="activity-row"><b>
                                                                        <%=ViewSupport.h(l.title())%>
                                                                    </b>
                                                                    <%if(!student||enrolled){%><a class="text-link"
                                                                            href="<%=base%>/lesson?id=<%=l.id()%>">Xem
                                                                            bài →</a>
                                                                        <%}else{%><span class="muted">Đăng ký để
                                                                                học</span>
                                                                            <%}%>
                                                                </div>
                                                                <%}if(teacher){%>
                                                                    <form class="editor-form" method="post"
                                                                        action="<%=base%>/courses"><input type="hidden"
                                                                            name="csrf"
                                                                            value="<%=ViewSupport.csrf(request)%>"><input
                                                                            type="hidden" name="id"
                                                                            value="<%=course.id()%>"><input
                                                                            type="hidden" name="chapter"
                                                                            value="<%=ch.id()%>"><input type="hidden"
                                                                            name="action" value="save-chapter">
                                                                        <div class="form-grid"><label>Tên chương<input
                                                                                    name="title"
                                                                                    value="<%=ViewSupport.h(ch.title())%>"
                                                                                    maxlength="150"
                                                                                    required></label><label>Thứ tự<input
                                                                                    type="number" name="order" min="1"
                                                                                    max="999" value="<%=ch.order()%>"
                                                                                    required></label></div><button
                                                                            class="button secondary">Lưu chương</button>
                                                                    </form>
                                                                    <form method="post" action="<%=base%>/courses"
                                                                        data-confirm="Xóa chương trống này?"><input
                                                                            type="hidden" name="csrf"
                                                                            value="<%=ViewSupport.csrf(request)%>"><input
                                                                            type="hidden" name="id"
                                                                            value="<%=course.id()%>"><input
                                                                            type="hidden" name="chapter"
                                                                            value="<%=ch.id()%>"><input type="hidden"
                                                                            name="action" value="delete-chapter"><button
                                                                            class="text-button">Xóa chương
                                                                            trống</button></form>
                                                                    <%}%>
                                                    </details>
                                                    <%}%>
                                                        <%if(teacher){%>
                                                            <form method="post" action="<%=base%>/courses"
                                                                class="editor-form">
                                                                <h3>Thêm chương</h3><input type="hidden" name="csrf"
                                                                    value="<%=ViewSupport.csrf(request)%>"><input
                                                                    type="hidden" name="id"
                                                                    value="<%=course.id()%>"><input type="hidden"
                                                                    name="action" value="save-chapter">
                                                                <div class="form-grid"><label>Tên chương<input
                                                                            name="title" maxlength="150"
                                                                            required></label><label>Thứ tự<input
                                                                            type="number" name="order"
                                                                            value="<%=chapters.size()+1%>" min="1"
                                                                            max="999" required></label></div><button
                                                                    class="button secondary">Thêm chương</button>
                                                            </form>
                                                            <%}%>
                                        </section>
                                        <%if(admin){%>
                                            <form class="surface editor-form" method="post" action="<%=base%>/courses"
                                                data-confirm="Áp dụng quyết định kiểm duyệt khóa học?">
                                                <h2>Quyết định kiểm duyệt</h2><input type="hidden" name="csrf"
                                                    value="<%=ViewSupport.csrf(request)%>"><input type="hidden"
                                                    name="id" value="<%=course.id()%>"><input type="hidden"
                                                    name="version" value="<%=course.version()%>"><input type="hidden"
                                                    name="action" value="moderate"><label>Quyết định<select
                                                        name="status">
                                                        <option value="published">Duyệt và xuất bản</option>
                                                        <option value="rejected">Từ chối, yêu cầu giáo viên sửa</option>
                                                        <option value="hidden">Ẩn khỏi học sinh</option>
                                                        <option value="archived">Gỡ và lưu trữ</option>
                                                    </select></label><label>Lý do / nhận xét kiểm duyệt<textarea
                                                        name="reason" maxlength="1000" required
                                                        rows="3"></textarea></label><button class="button primary">Lưu
                                                    quyết định</button>
                                            </form>
                                            <%}%>
                                                <%}else{List<CatalogEntry> courses=(List<CatalogEntry>
                                                        )request.getAttribute("courses");%><div class="toolbar"><label
                                                                class="search-field"><input type="search" data-search
                                                                    aria-label="Tìm khóa học"
                                                                    placeholder="Tìm khóa học, chủ đề hoặc giáo viên"></label><label>Trạng
                                                                thái<select data-filter>
                                                                    <option value="all">Tất cả</option>
                                                                    <option value="published">Đã xuất bản</option>
                                                                    <%if(!student){%>
                                                                        <option value="pending">Chờ duyệt</option>
                                                                        <option value="draft">Bản nháp</option>
                                                                        <option value="rejected">Từ chối</option>
                                                                        <option value="hidden">Đã ẩn</option>
                                                                        <option value="archived">Lưu trữ</option>
                                                                        <%}%>
                                                                </select></label><span data-result-count>
                                                                <%=courses.size()%> nội dung
                                                            </span></div>
                                                        <div class="resource-grid">
                                                            <%int ci=0;for(CatalogEntry c:courses){%>
                                                                <article class="resource-card" data-search-item
                                                                    data-state="<%=c.status()%>">
                                                                    <div class="course-banner">
                                                                        <%if(!cs.cover(c.id()).isBlank()){%><img
                                                                                class="course-cover-image"
                                                                                src="<%=ViewSupport.h(cs.cover(c.id()))%>"
                                                                                alt="Ảnh khóa học <%=ViewSupport.h(c.title())%>"
                                                                                loading="lazy" data-image-fallback><span
                                                                                class="image-error" hidden>Ảnh nguồn
                                                                                không tải được</span>
                                                                            <%}%><span>0<%=++ci%></span><b>
                                                                                    <%=ViewSupport.h(c.topic())%>
                                                                                </b><span>F = ma</span>
                                                                    </div><span class="badge">
                                                                        <%=ViewSupport.status(c.status())%>
                                                                    </span>
                                                                    <h2>
                                                                        <%=ViewSupport.h(c.title())%>
                                                                    </h2>
                                                                    <p class="clamp">
                                                                        <%=ViewSupport.h(c.body())%>
                                                                    </p>
                                                                    <p>Giáo viên: <%=ViewSupport.h(c.owner())%>
                                                                    </p>
                                                                    <p>
                                                                        <%=cs.chapters(c.id()).size()%> chương ·
                                                                            <%=cs.lessons(c.id()).size()%> bài đã xuất
                                                                                bản
                                                                    </p><a class="button secondary"
                                                                        href="<%=base%>/courses?id=<%=c.id()%>">
                                                                        <%=student&&cs.enrolled(viewer.getEmail(),c.id())?"Đã tham gia · tiếp tục học →":"Xem khóa học →"%>
                                                                    </a>
                                                                </article>
                                                                <%}%>
                                                        </div>
                                                        <div class="empty-state" data-empty
                                                            <%=courses.isEmpty()?"":"hidden"%>><h2>Chưa có khóa học phù
                                                                hợp</h2>
                                                            <p>Đổi bộ lọc hoặc quay lại khi giáo viên mở khóa học.</p>
                                                        </div>
                                                        <%}%>
                                                            <%@ include file="end.jspf" %>