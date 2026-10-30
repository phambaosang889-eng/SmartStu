<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" import="com.smartstudy.model.view.AIStatus" %>
    <%@ include file="start.jspf" %>
        <%AIStatus ai=(AIStatus)request.getAttribute("aiStatus");%>
            <section class="page-heading">
                <div><span class="eyebrow">TÍCH HỢP PHÍA MÁY CHỦ</span>
                    <h1>Trạng thái dịch vụ AI</h1>
                    <p>AI hỗ trợ biên soạn và đánh giá; giáo viên kiểm duyệt câu hỏi và kết quả trước khi công bố.</p>
                </div>
            </section>
            <%if(request.getAttribute("aiError")!=null){%>
                <div class="notice error" role="alert">
                    <%=ViewSupport.h(request.getAttribute("aiError"))%>
                </div>
                <%}%>
                    <div class="notice"><b>
                            <%=ai.configured()?ai.enabled()?"Đã cấu hình kết nối":"Dịch vụ đang tạm dừng":"Chưa kết nối dịch vụ AI"%>
                        </b><span>
                            <%=ViewSupport.h(request.getAttribute("aiProvider"))%>
                        </span><span>
                            <%=ViewSupport.h(ai.lastState())%>
                        </span></div>
                    <div class="stat-grid">
                        <div class="stat-card"><span>Yêu cầu đã phản hồi</span><strong>
                                <%=ai.successes()%>
                            </strong><span>Cần giáo viên rà soát kết quả</span></div>
                        <div class="stat-card"><span>Lỗi đã ghi nhận</span><strong>
                                <%=ai.failures()%>
                            </strong><span>Lỗi mạng, timeout hoặc cấu trúc phản hồi</span></div>
                        <div class="stat-card"><span>Giới hạn / 5 phút</span><strong>
                                <%=ai.limit()%>
                            </strong><span>Theo từng người dùng</span></div>
                        <div class="stat-card"><span>Thời gian chờ tối đa</span><strong>25 giây</strong><span>Có thể
                                tiếp tục làm việc thủ công khi lỗi</span></div>
                    </div>
                    <%if(viewer.getRole()==Role.ADMIN){%>
                        <form class="surface editor-form" method="post" action="<%=base%>/ai-settings">
                            <h2>Chính sách sử dụng</h2><input type="hidden" name="csrf"
                                value="<%=ViewSupport.csrf(request)%>"><label class="check-line"><input type="checkbox"
                                    name="enabled" <%=ai.enabled()?"checked":""%>> Cho phép sử dụng dịch vụ khi đủ cấu
                                hình</label><label>Số yêu cầu tối đa / 5 phút / người dùng<input type="number"
                                    name="limit" value="<%=ai.limit()%>" min="1" max="30" required></label><button
                                class="button primary">Lưu chính sách</button>
                        </form>
                        <section class="surface">
                            <h2>Cấu hình kết nối</h2>
                            <p>Với Google Gemini, đặt SMARTSTU_AI_PROVIDER=gemini, GEMINI_API_KEY và
                                SMARTSTU_GEMINI_MODEL trong biến môi trường hoặc file cấu hình riêng của máy chủ, rồi
                                khởi động lại ứng dụng. Không cần gateway trung gian. Xem docs/GEMINI.md để thiết lập.
                                Cấu hình đầy đủ chỉ xác nhận đã có khóa; kết nối được xác minh sau khi nhận phản hồi
                                thành công. Gateway cũ vẫn dùng được với SMARTSTU_AI_PROVIDER=gateway.</p>
                            <p>Khóa chỉ được đọc ở backend. Giao diện không hiển thị hoặc lưu khóa. Nhật ký lỗi được xem
                                ở Nhật ký hệ thống.</p>
                        </section>
                        <%}else{%>
                            <section class="surface">
                                <h2>Khi dịch vụ chưa khả dụng</h2>
                                <p>Bạn có thể dùng bộ câu hỏi mẫu có nhãn, chỉnh sửa thủ công và chấm tự luận theo
                                    rubric. Liên hệ quản trị viên để cấu hình kết nối AI.</p><a class="button primary"
                                    href="<%=base%>/ai-questions">Chuẩn bị câu hỏi →</a>
                            </section>
                            <%}%>
                                <%@ include file="end.jspf" %>