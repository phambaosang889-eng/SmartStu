<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"
    import="com.smartstudy.model.CatalogEntry,com.smartstudy.service.*,com.smartstudy.model.view.*" %>
    <%@ include file="start.jspf" %>
        <%boolean student=viewer.getRole()==Role.STUDENT,admin=viewer.getRole()==Role.ADMIN;List<DashboardMetric>
            metrics=(List<DashboardMetric>)request.getAttribute("metrics");List<ReportingService.CourseRow>
                    courseRows=(List<ReportingService.CourseRow>)request.getAttribute("courseRows");%>
                        <section class="hero">
                            <div><span class="eyebrow">
                                    <%=current.equals("dashboard")?"SMART LEARNING · VẬT LÝ 10":"TIẾN ĐỘ & KẾT QUẢ"%>
                                </span>
                                <h1>
                                    <%=current.equals("dashboard")?"Xin chào, "+ViewSupport.h(viewer.getName()):admin?" Báo cáo toàn hệ thống":"Theo dõi kết quả học tập"%>
                                </h1>
                                <p>
                                    <%=student?"Mỗi ngày một chút, hiểu Vật lý nhiều hơn. Tiếp tục hành trình của bạn.":admin?"Theo dõi người dùng, kiểm duyệt khóa học và giám sát hoạt động nền tảng.":"Quản lý khóa học, theo dõi học sinh và phản hồi bài làm trong phạm vi phụ trách."%>
                                </p>
                                <div class="actions hero-actions"><a class="button primary" href="<%=base%>/<%=admin?" courses":student?(Boolean.TRUE.equals(request.getAttribute("hasLearningContent"))?"lesson":"courses"):"essays"%>">
                                        <%=admin?"Kiểm duyệt khóa học →":student?(Boolean.TRUE.equals(request.getAttribute("hasLearningContent"))?"Tiếp tục học →":"Khám phá khóa học →"):"Xem bài nộp →"%></a>
                                    <%if(viewer.getRole()==Role.TEACHER){%><a class="button secondary"
                                            href="<%=base%>/ai-questions">✦ AI tạo câu hỏi</a>
                                        <%}%>
                                </div>
                            </div>
                            <div class="hero-symbol" aria-hidden="true"><span>F = ma</span></div>
                        </section>
                        <%if(request.getAttribute("reportError")!=null){%>
                            <div class="notice error" role="alert">
                                <%=ViewSupport.h(request.getAttribute("reportError"))%>
                            </div>
                            <%}%>
                                <div class="stat-grid">
                                    <%for(DashboardMetric m:metrics){%>
                                        <div class="stat-card"><span>
                                                <%=ViewSupport.h(m.label())%>
                                            </span><strong>
                                                <%=ViewSupport.h(m.value())%>
                                            </strong><span>
                                                <%=ViewSupport.h(m.note())%>
                                            </span></div>
                                        <%}%>
                                </div>
                                <%if(admin){%>
                                    <%@ include file="growth.jspf" %>
                                        <%}%>
                                            <div class="split-grid">
                                                <section class="surface">
                                                    <h2>Hoạt động 7 ngày gần đây</h2>
                                                    <p>Một nhịp nhìn lại những gì bạn đã thực hiện trong tuần.</p>
                                                    <%List<ChartBar> chart=(List<ChartBar>
                                                            )request.getAttribute("chart");%><div class="bar-chart"
                                                                role="img" aria-label="Biểu đồ hoạt động 7 ngày">
                                                                <%for(ChartBar day:chart){%>
                                                                    <div class="bar-column"><strong>
                                                                            <%=day.count()%>
                                                                        </strong>
                                                                        <div class="bar-track">
                                                                            <div class="bar-fill"
                                                                                style="height:<%=day.percent()%>%">
                                                                            </div>
                                                                        </div><span>
                                                                            <%=day.day().format(java.time.format.DateTimeFormatter.ofPattern("dd /MM"))%>
                                                                        </span>
                                                                    </div>
                                                                    <%}%>
                                                            </div>
                                                            <details>
                                                                <summary>Xem số liệu dạng bảng</summary>
                                                                <div class="table-wrap">
                                                                    <table>
                                                                        <thead>
                                                                            <tr>
                                                                                <th>Ngày</th>
                                                                                <th>Hoạt động</th>
                                                                            </tr>
                                                                        </thead>
                                                                        <tbody>
                                                                            <%for(var day:chart){%>
                                                                                <tr>
                                                                                    <td>
                                                                                        <%=day.day()%>
                                                                                    </td>
                                                                                    <td>
                                                                                        <%=day.count()%>
                                                                                    </td>
                                                                                </tr>
                                                                                <%}%>
                                                                        </tbody>
                                                                    </table>
                                                                </div>
                                                            </details>
                                                </section>
                                                <section class="surface">
                                                    <h2>Hoạt động gần đây</h2>
                                                    <%List<LearningService.Activity> activities=(List
                                                        <LearningService.Activity>
                                                            )request.getAttribute("activities");if(activities.isEmpty()){%>
                                                            <div class="empty-state compact">
                                                                <h3>Chưa có hoạt động</h3>
                                                                <p>Học bài, làm quiz hoặc quản lý khóa học để bắt đầu
                                                                    ghi nhận.</p>
                                                            </div>
                                                            <%}for(var activity:activities){%>
                                                                <div class="activity-row">
                                                                    <div><b>
                                                                            <%=ViewSupport.h(activity.title())%>
                                                                        </b>
                                                                        <p>
                                                                            <%=ViewSupport.h(activity.detail())%>
                                                                        </p>
                                                                    </div><time>
                                                                        <%=ViewSupport.date(activity.time())%>
                                                                    </time>
                                                                </div>
                                                                <%}%>
                                                </section>
                                            </div>
                                            <section class="surface">
                                                <div class="page-heading">
                                                    <div>
                                                        <h2>
                                                            <%=student?"Tiến độ khóa học":"Kết quả theo khóa học"%>
                                                        </h2>
                                                        <p>Lọc theo ngày đăng ký, ngày làm quiz và ngày nộp tự luận.
                                                            Tiến độ thể hiện trạng thái hiện tại của các đăng ký trong
                                                            khoảng đã chọn.</p>
                                                    </div>
                                                    <%if(!student){%><a class="button secondary"
                                                            href="<%=base%>/<%=current%>?export=csv<%=request.getParameter(" course")==null?"":"&amp;course="+ViewSupport.h(request.getParameter(" course"))%>
                                                            <%=admin&&request.getParameter("teacher")!=null?"&amp;teacher="+java.net.URLEncoder.encode(request.getParameter("teacher"),java.nio.charset.StandardCharsets.UTF_8):""%>
                                                                <%=request.getParameter("from")==null?"":"&amp;from="+ViewSupport.h(request.getParameter("from"))%>
                                                                    <%=request.getParameter("to")==null?"":"&amp;to="+ViewSupport.h(request.getParameter("to"))%>
                                                                        ">Xuất báo cáo CSV
                                                        </a>
                                                        <%}%>
                                                </div>
                                                <form method="get" action="<%=base%>/<%=current%>" class="toolbar">
                                                    <label>Khóa học<select name="course">
                                                            <option value="">Tất cả khóa học trong phạm vi</option>
                                                            <%for(var c:(List<CatalogEntry>
                                                                )request.getAttribute("reportCourses")){%><option
                                                                    value="<%=c.id()%>"
                                                                    <%=c.id().equals(request.getParameter("course"))?"selected":""%>
                                                                    ><%=ViewSupport.h(c.title())%>
                                                                </option>
                                                                <%}%>
                                                        </select></label>
                                                    <%if(admin){%><label>Giáo viên<select name="teacher">
                                                                <option value="">Tất cả giáo viên</option>
                                                                <%for(var a:(List<UserStore.Account>
                                                                    )request.getAttribute("reportTeachers")){%><option
                                                                        value="<%=ViewSupport.h(a.user().getEmail())%>"
                                                                        <%=a.user().getEmail().equals(request.getParameter("teacher"))?"selected":""%>
                                                                        ><%=ViewSupport.h(a.user().getName())%>
                                                                    </option>
                                                                    <%}%>
                                                            </select></label>
                                                        <%}%><label>Từ ngày<input type="date" name="from"
                                                                    value="<%=ViewSupport.h(request.getParameter(" from"))%>"></label><label>Đến ngày<input type="date"
                                                                    name="to"
                                                                    value="<%=ViewSupport.h(request.getParameter(" to"))%>"></label><button
                                                                class="button secondary">Lọc báo cáo</button>
                                                </form>
                                                <div class="table-wrap">
                                                    <table>
                                                        <thead>
                                                            <tr>
                                                                <th>Khóa học</th>
                                                                <th>Tham gia</th>
                                                                <th>Hoàn thành</th>
                                                                <th>Tiến độ TB</th>
                                                                <th>Điểm quiz TB</th>
                                                                <th>Tự luận chờ</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody>
                                                            <%for(var row:courseRows){%>
                                                                <tr>
                                                                    <td><a class="text-link"
                                                                            href="<%=base%>/courses?id=<%=row.id()%>">
                                                                            <%=ViewSupport.h(row.title())%>
                                                                        </a><small class="muted">
                                                                            <%=ViewSupport.h(row.owner())%>
                                                                        </small></td>
                                                                    <td>
                                                                        <%=row.enrolled()%>
                                                                    </td>
                                                                    <td>
                                                                        <%=row.completed()%>
                                                                    </td>
                                                                    <td>
                                                                        <%=row.percent()%>%
                                                                    </td>
                                                                    <td>
                                                                        <%=row.average()%>
                                                                    </td>
                                                                    <td>
                                                                        <%=row.pending()%>
                                                                    </td>
                                                                </tr>
                                                                <%}%>
                                                        </tbody>
                                                    </table>
                                                </div>
                                                <%if(courseRows.isEmpty()){%>
                                                    <p>Chưa có khóa học phù hợp bộ lọc.</p>
                                                    <%}%>
                                            </section>
                                            <div class="split-grid">
                                                <section class="surface">
                                                    <h2>
                                                        <%=student?"Bài học tiếp theo":"Học liệu trong phạm vi quản lý"%>
                                                    </h2>
                                                    <%List<CatalogEntry> lessons=(List<CatalogEntry>
                                                            )request.getAttribute("nextLessons");Set<String> done=(Set
                                                                <String>
                                                                    )request.getAttribute("completed");if(lessons.isEmpty()){%>
                                                                    <p>
                                                                        <%=student?"Đăng ký khóa học hoặc chờ giáo viên xuất bản bài học mới.":"Tạo khóa học và chương để bắt đầu biên soạn bài học."%>
                                                                    </p><a class="text-link" href="<%=base%>/courses">
                                                                        <%=student?"Khám phá khóa học →":"Quản lý khóa học →"%>
                                                                    </a>
                                                                    <%}for(var lesson:lessons){%>
                                                                        <div class="activity-row">
                                                                            <div><b>
                                                                                    <%=ViewSupport.h(lesson.title())%>
                                                                                </b>
                                                                                <p>
                                                                                    <%=ViewSupport.h(lesson.topic())%>
                                                                                </p>
                                                                            </div><a class="button secondary"
                                                                                href="<%=base%>/lesson?id=<%=lesson.id()%>">
                                                                                <%=student&&done.contains(lesson.id())?"Ôn lại":"Mở bài →"%>
                                                                            </a>
                                                                        </div>
                                                                        <%}%>
                                                </section>
                                                <section class="surface">
                                                    <h2>Gợi ý ôn tập từ kết quả quiz</h2>
                                                    <%List<ReportingService.TopicRow> topics=(List
                                                        <ReportingService.TopicRow>
                                                            )request.getAttribute("topics");if(topics.isEmpty()){%><p>
                                                                Chưa đủ dữ liệu để xác định điểm mạnh và phần cần ôn.
                                                                Hoàn thành quiz để nhận gợi ý.</p>
                                                            <%}for(var topic:topics){%>
                                                                <div class="activity-row">
                                                                    <div><b>
                                                                            <%=ViewSupport.h(topic.topic())%>
                                                                        </b>
                                                                        <p>
                                                                            <%=topic.wrong()%>/<%=topic.total()%> câu
                                                                                    cần ôn · <%=topic.wrong()==0?"Nắm tốt các câu đã làm":"Đọc lại lý thuyết và lời giải các câu sai"%>
                                                                        </p>
                                                                    </div>
                                                                </div>
                                                                <%}%><a class="text-link" href="<%=base%>/quiz">Xem quiz
                                                                        và kết quả →</a>
                                                </section>
                                            </div>
                                            <%@ include file="end.jspf" %>