<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.service.*" %>
    <%@ include file="start.jspf" %>
        <%boolean
            admin=viewer.getRole()==Role.ADMIN,profile=current.equals("profile"),audit=current.equals("audit");UserStore.Account
            account=(UserStore.Account)request.getAttribute("account");boolean
            edit=admin&&!profile&&!audit&&("edit".equals(request.getParameter("mode"))||"save-account".equals(request.getParameter("action")));boolean
            err=request.getAttribute("accountError")!=null;%>
            <section class="page-heading">
                <div><span class="eyebrow">
                        <%=profile?"TÀI KHOẢN CỦA BẠN":admin?"QUẢN TRỊ NGƯỜI DÙNG":"PHẠM VI KHÓA HỌC PHỤ TRÁCH"%>
                    </span>
                    <h1>
                        <%=profile?"Hồ sơ và bảo mật":audit?"Nhật ký hệ thống":edit?account==null?"Tạo tài khoản":"Cập nhật tài khoản":admin?current.equals("teachers")?"Giáo viên":"Tất cả tài khoản":"Học sinh tham gia khóa học"%>
                    </h1>
                    <p>
                        <%=profile?"Cập nhật tên hiển thị và đổi mật khẩu bằng mật khẩu hiện tại.":admin?"Quản lý quyền truy cập và theo dõi hoạt động quản trị.":"Chỉ học sinh đăng ký khóa học do bạn phụ trách xuất hiện tại đây."%>
                    </p>
                </div>
                <%if(admin&&!profile&&!audit&&!edit){%><a class="button primary" href="<%=base%>/users?mode=edit">＋ Tạo
                        tài khoản</a>
                    <%}%>
            </section>
            <%if(err){%>
                <div class="notice error" role="alert">
                    <%=ViewSupport.h(request.getAttribute("accountError"))%>
                </div>
                <%}%>
                    <%if(profile){%>
                        <div class="split-grid">
                            <form class="surface editor-form" method="post" action="<%=base%>/profile">
                                <h2>Thông tin cá nhân</h2><input type="hidden" name="csrf"
                                    value="<%=ViewSupport.csrf(request)%>"><input type="hidden" name="action"
                                    value="profile">
                                <p>
                                    <%=ViewSupport.h(viewer.getEmail())%> · <%=ViewSupport.roleLabel(viewer)%>
                                </p><label>Tên hiển thị<input name="name" maxlength="100"
                                        value="<%=ViewSupport.h(viewer.getName())%>" required></label><button
                                    class="button primary">Lưu hồ sơ</button>
                            </form>
                            <form class="surface editor-form" method="post" action="<%=base%>/profile">
                                <h2>Đổi mật khẩu</h2><input type="hidden" name="csrf"
                                    value="<%=ViewSupport.csrf(request)%>"><input type="hidden" name="action"
                                    value="password"><label>Mật khẩu hiện tại<input type="password"
                                        name="currentPassword" autocomplete="current-password" maxlength="200"
                                        required></label><label>Mật khẩu mới<input type="password" name="newPassword"
                                        autocomplete="new-password" minlength="8" maxlength="200"
                                        required></label><label>Nhập lại mật khẩu mới<input type="password"
                                        name="confirmPassword" autocomplete="new-password" minlength="8" maxlength="200"
                                        required></label><button class="button primary">Đổi mật khẩu</button>
                            </form>
                        </div>
                        <details class="storage-note">
                            <summary>Thông tin phiên bản</summary>
                            <p>
                                <%=com.smartstudy.persistence.Database.memory()?"Chế độ bộ nhớ: dữ liệu đặt lại khi ứng dụng khởi động lại.":"Dữ liệu tài khoản và học tập được lưu trong MySQL. Phiên đăng nhập có thể hết hạn khi máy chủ khởi động lại."%>
                            </p>
                        </details>
                        <%}else if(audit){%>
                            <section class="surface">
                                <h2>Đăng nhập và thao tác quản trị</h2>
                                <%for(AuditService.Event event:AuditService.events()){%>
                                    <div class="activity-row">
                                        <div><b>
                                                <%=ViewSupport.h(event.action())%>
                                            </b>
                                            <p>
                                                <%=ViewSupport.h(event.actor())%> · <%=ViewSupport.h(event.target())%>
                                            </p>
                                        </div><time>
                                            <%=ViewSupport.date(event.time())%>
                                        </time>
                                    </div>
                                    <%}if(AuditService.events().isEmpty()){%>
                                        <p>Chưa ghi nhận hoạt động.</p>
                                        <%}%>
                            </section>
                            <%}else if(edit){%>
                                <form class="surface editor-form" method="post" action="<%=base%>/users"
                                    data-confirm="Lưu thông tin và quyền truy cập của tài khoản này?"><input
                                        type="hidden" name="csrf" value="<%=ViewSupport.csrf(request)%>"><input
                                        type="hidden" name="action" value="save-account"><input type="hidden"
                                        name="version" value="<%=account==null?0:account.version()%>"><label>Email<input
                                            type="email" name="email" maxlength="150" required
                                            <%=account==null?"":"readonly"%> value="
                                        <%=ViewSupport.h(account==null?(err?request.getParameter("email"):""):account.user().getEmail())%>
                                            "></label><label>Tên hiển thị<input name="name" maxlength="100" required
                                            value="<%=ViewSupport.h(err?request.getParameter(" name"):account==null?"":account.user().getName())%>"></label>
                                    <div class="form-grid"><label>Vai trò<select name="role">
                                                <%for(Role role:Role.values()){%>
                                                    <option value="<%=role%>"
                                                        <%=account!=null&&account.user().getRole()==role?"selected":""%>
                                                        ><%=role==Role.STUDENT?"Học sinh":role==Role.TEACHER?"Giáo viên":"Quản trị viên"%>
                                                    </option>
                                                    <%}%>
                                            </select></label><label>Trạng thái<select name="status">
                                                <option value="active">Hoạt động</option>
                                                <option value="locked"
                                                    <%=account!=null&&account.locked()?"selected":""%>>Khóa truy cập
                                                </option>
                                            </select></label></div>
                                    <%if(account==null){%><label>Mật khẩu ban đầu<input type="password" name="password"
                                                autocomplete="new-password" minlength="8" maxlength="200"
                                                required></label>
                                        <p>Mật khẩu được băm bằng PBKDF2 và không hiển thị lại sau khi lưu.</p>
                                        <%}else{%>
                                            <p>Người dùng tự đổi mật khẩu trong hồ sơ bằng mật khẩu hiện tại. Không cung
                                                cấp chức năng xem lại mật khẩu.</p>
                                            <%}%>
                                                <div class="actions"><a class="button secondary"
                                                        href="<%=base%>/users">Hủy</a><button class="button primary">Lưu
                                                        tài khoản</button></div>
                                </form>
                                <%}else{List<UserStore.Account> accounts=(List<UserStore.Account>
                                        )request.getAttribute("accounts");%><div class="toolbar"><label
                                                class="search-field"><input type="search" data-search
                                                    aria-label="Tìm tài khoản"
                                                    placeholder="Tìm tên, email hoặc vai trò"></label><label>Trạng
                                                thái<select data-filter>
                                                    <option value="all">Tất cả</option>
                                                    <option value="active">Hoạt động</option>
                                                    <option value="locked">Bị khóa</option>
                                                </select></label><span data-result-count>
                                                <%=accounts.size()%> nội dung
                                            </span></div>
                                        <div class="resource-grid">
                                            <%for(UserStore.Account a:accounts){User person=a.user();%>
                                                <article class="resource-card" data-search-item
                                                    data-state="<%=a.locked()?" locked":"active"%>"><span class="badge">
                                                        <%=ViewSupport.roleLabel(person)%> · <%=a.locked()?"Bị khóa":"Hoạt động"%>
                                                    </span>
                                                    <h2>
                                                        <%=ViewSupport.h(person.getName())%>
                                                    </h2>
                                                    <p>
                                                        <%=ViewSupport.h(person.getEmail())%>
                                                    </p>
                                                    <%if(person.getRole()==Role.STUDENT){for(var
                                                        enrollment:CourseService.getInstance().enrollments(viewer).stream().filter(e->
                                                        e.active()&&e.email().equals(person.getEmail())).toList()){%><p>
                                                            <%=ViewSupport.h(CatalogService.getInstance().get(enrollment.course()).title())%>
                                                                ·
                                                                <%=Math.round(CourseService.getInstance().progress(person.getEmail(),enrollment.course()))%>
                                                                    % hoàn thành
                                                        </p>
                                                        <%}%>
                                                            <%@ include file="pupil-stats.jspf" %>
                                                                <%}if(person.getRole()==Role.TEACHER){%>
                                                                    <p>
                                                                        <%=CatalogService.getInstance().list("courses",person).size()%>
                                                                            khóa học phụ trách ·
                                                                            <%=CourseService.getInstance().students(person).size()%>
                                                                                học sinh
                                                                    </p>
                                                                    <%@ include file="teacher-stats.jspf" %>
                                                                        <%}if(admin){%>
                                                                            <p>Đăng nhập gần nhất:
                                                                                <%=ViewSupport.date(a.lastLogin())%>
                                                                            </p><a class="button secondary"
                                                                                href="<%=base%>/users?mode=edit&email=<%=java.net.URLEncoder.encode(person.getEmail(),java.nio.charset.StandardCharsets.UTF_8)%>">Sửa
                                                                                / khóa / đổi vai trò</a>
                                                                            <%}else{%>
                                                                                <p>Kết quả chi tiết được xem trong Quiz
                                                                                    và Bài tự luận thuộc khóa học của
                                                                                    bạn.</p>
                                                                                <%}%>
                                                </article>
                                                <%}%>
                                        </div>
                                        <div class="empty-state" data-empty <%=accounts.isEmpty()?"":"hidden"%>><h2>Chưa
                                                có người dùng phù hợp</h2>
                                            <p>Đổi bộ lọc hoặc chờ học sinh đăng ký khóa học.</p>
                                        </div>
                                        <%}%>
                                            <%@ include file="end.jspf" %>