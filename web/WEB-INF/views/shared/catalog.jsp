<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"
    import="com.smartstudy.model.CatalogEntry,com.smartstudy.service.CourseService" %>
    <%@ include file="start.jspf" %>
        <% String label=(String)request.getAttribute("catalogLabel");String
            kind=(String)request.getAttribute("catalogKind");CatalogEntry
            entry=(CatalogEntry)request.getAttribute("entry");boolean
            writable=viewer.getRole()==Role.TEACHER||kind.equals("notifications")||kind.equals("topics");boolean
            editing=Boolean.TRUE.equals(request.getAttribute("editing"))||"edit".equals(request.getParameter("mode"));boolean
            error=request.getAttribute("formError")!=null; List<CatalogEntry> items=(List<CatalogEntry>
                )request.getAttribute("entries");%>
                <section class="page-heading">
                    <div><span class="eyebrow">QUẢN LÝ THƯ VIỆN</span>
                        <h1>
                            <%=editing?(entry==null?"Thêm ":" Chỉnh sửa ")+label.toLowerCase():label%></h1><p><%=kind.equals(" students")||kind.equals("teachers")?"Danh bạ lớp học demo. Việc sửa danh bạ không tạo hoặc thay đổi tài khoản đăng nhập.":"Biên soạn nội dung rõ ràng và xuất bản khi đã sẵn sàng."%>
                                </p>
                    </div>
                    <%if(!editing&&writable){%><a class="button primary" href="<%=base%>/<%=current%>?mode=edit">＋ Thêm
                            mới</a>
                        <%}else if(editing){%><a class="button secondary" href="<%=base%>/<%=current%>">← Danh sách</a>
                            <%if(kind.equals("lesson")&&viewer.getRole()==Role.TEACHER){%><a class="button primary"
                                    href="#lesson-video-manager">Video bài giảng ↓</a>
                                <%}%>
                                    <%}%>
                </section>
                <% if(error){%>
                    <div class="notice error" role="alert">
                        <%=ViewSupport.h(request.getAttribute("formError"))%>
                    </div>
                    <%}%>
                        <%if(editing){String et=error?request.getParameter("title"):entry==null?"":entry.title();String
                            eb=error?request.getParameter("body"):entry==null?"":entry.body();String
                            ep=error?request.getParameter("topic"):entry==null?"Vật lý 10":entry.topic();String
                            es=error?request.getParameter("status"):entry==null?"draft":entry.status();%>
                            <form class="surface editor-form" method="post" action="<%=base%>/<%=current%>"><input
                                    type="hidden" name="csrf" value="<%=ViewSupport.csrf(request)%>"><input
                                    type="hidden" name="action" value="save"><input type="hidden" name="id"
                                    value="<%=entry==null?"":entry.id()%>"><input type="hidden" name="version"
                                    value="<%=entry==null?0:entry.version()%>">
                                <div class="form-section">
                                    <h2>Thông tin cơ bản</h2>
                                    <p>Nội dung có thể lưu dưới dạng bản nháp trước khi xuất bản.</p>
                                </div>
                                <%if(viewer.getRole()==Role.TEACHER){%>
                                    <%@ include file="placement.jspf" %>
                                        <%}%><label>Tiêu đề / tên<input name="title" required maxlength="150"
                                                    value="<%=ViewSupport.h(et)%>" autofocus></label>
                                            <div class="form-grid"><label>Chủ đề / lớp<input name="topic" required
                                                        maxlength="80"
                                                        value="<%=ViewSupport.h(ep)%>"></label><label>Trạng thái<select
                                                        name="status">
                                                        <option value="draft" <%="draft" .equals(es)?"selected":""%>>Bản
                                                            nháp</option>
                                                        <option value="published" <%="published"
                                                            .equals(es)?"selected":""%>>Đã xuất bản / hoạt động</option>
                                                        <option value="archived" <%="archived"
                                                            .equals(es)?"selected":""%>>Lưu trữ</option>
                                                    </select></label></div><label>
                                                <%=kind.equals("flashcards")?"Mặt sau · câu trả lời":"Nội dung / thông tin chi tiết"%><textarea name="body" rows="14" maxlength="12000"
                                                        required data-count><%=ViewSupport.h(eb)%></textarea>
                                            </label><span class="field-help" data-count-output></span>
                                            <div class="form-footer"><a class="button secondary"
                                                    href="<%=base%>/<%=current%>">Hủy</a><button class="button primary"
                                                    type="submit">Lưu nội dung</button></div>
                            </form>
                            <%if(kind.equals("lesson")&&viewer.getRole()==Role.TEACHER){%>
                                <%@ include file="video-manager.jspf" %>
                                    <%}%>
                                        <%}else if(entry!=null){%>
                                            <article class="surface reading"><span class="badge">
                                                    <%=ViewSupport.h(entry.topic())%>
                                                </span>
                                                <h2>
                                                    <%=ViewSupport.h(entry.title())%>
                                                </h2>
                                                <%@ include file="lesson-videos.jspf" %>
                                                    <div class="prose">
                                                        <%=ViewSupport.h(entry.body())%>
                                                    </div>
                                                    <div class="form-footer"><a class="button secondary"
                                                            href="<%=base%>/<%=current%>">← Danh sách</a>
                                                        <%if(writable){%><a class="button primary"
                                                                href="<%=base%>/<%=current%>?mode=edit&id=<%=entry.id()%>">Chỉnh
                                                                sửa</a>
                                                            <%}%>
                                                    </div>
                                            </article>
                                            <%}else{%>
                                                <div class="toolbar"><label class="search-field"><span
                                                            aria-hidden="true">⌕</span><input type="search" data-search
                                                            placeholder="Tìm theo tên, nội dung hoặc chủ đề"
                                                            aria-label="Tìm kiếm nội dung"></label><label
                                                        class="filter-label">Trạng thái<select data-filter>
                                                            <%if(kind.equals("topics")){%>
                                                                <option value="active" selected>Đang sử dụng</option>
                                                                <%}%>
                                                                    <option value="all">Tất cả</option>
                                                                    <option value="published">Đã xuất bản</option>
                                                                    <option value="draft">Bản nháp</option>
                                                                    <option value="archived">Lưu trữ</option>
                                                        </select></label><span class="result-count" data-result-count>
                                                        <%=items.size()%> nội dung
                                                    </span></div>
                                                <div class="resource-grid">
                                                    <%for(CatalogEntry e:items){%>
                                                        <article class="resource-card" data-search-item
                                                            data-state="<%=e.status()%>"
                                                            <%=kind.equals("topics")&&e.status().equals("archived")?"hidden":""%>
                                                            ><div class="card-top"><span class="topic-label">
                                                                    <%=ViewSupport.h(e.topic())%>
                                                                </span><span class="badge <%=e.status()%>">
                                                                    <%=e.status().equals("published")?"Đã xuất bản":e.status().equals("draft")?"Bản nháp":"Lưu trữ"%>
                                                                </span></div>
                                                            <h2><a href="<%=base%>/<%=current%>?id=<%=e.id()%>">
                                                                    <%=ViewSupport.h(e.title())%>
                                                                </a></h2>
                                                            <p class="clamp">
                                                                <%=ViewSupport.h(e.body())%>
                                                            </p>
                                                            <div class="actions">
                                                                <%if(writable){%><a class="button secondary"
                                                                        href="<%=base%>/<%=current%>?mode=edit&id=<%=e.id()%>">Chỉnh
                                                                        sửa</a>
                                                                    <%}%><a class="text-link"
                                                                            href="<%=base%>/<%=current%>?id=<%=e.id()%>">Xem
                                                                            chi tiết →</a>
                                                            </div>
                                                            <%if(writable){%>
                                                                <form class="archive-form" method="post"
                                                                    action="<%=base%>/<%=current%>"
                                                                    data-confirm="<%=e.status().equals(" archived")?"Khôi phục nội dung thành bản nháp?":"Lưu trữ nội dung này? Bạn có thể khôi phục sau."%>
                                                                    "><input type="hidden" name="csrf"
                                                                        value="<%=ViewSupport.csrf(request)%>"><input
                                                                        type="hidden" name="id"
                                                                        value="<%=e.id()%>"><input type="hidden"
                                                                        name="version" value="<%=e.version()%>"><input
                                                                        type="hidden" name="action"
                                                                        value="<%=e.status().equals(" archived")?"restore":"archive"%>"><button
                                                                        class="text-button" type="submit">
                                                                        <%=e.status().equals("archived")?"Khôi phục":"Lưu trữ"%>
                                                                    </button></form>
                                                                <%}%>
                                                        </article>
                                                        <%}%>
                                                </div>
                                                <div class="empty-state" data-empty <%=items.isEmpty()?"":"hidden"%>
                                                    ><span>⌕</span>
                                                    <h2>Chưa có nội dung phù hợp</h2>
                                                    <p>Thử từ khóa khác, đổi bộ lọc hoặc thêm nội dung đầu tiên.</p>
                                                    <%if(writable){%><a class="button primary"
                                                            href="<%=base%>/<%=current%>?mode=edit">Thêm mới</a>
                                                        <%}%>
                                                </div>
                                                <%}%>
                                                    <%@ include file="end.jspf" %>