<%@ page pageEncoding="UTF-8" import="com.smartstudy.dao.ConversationDao,java.util.*" %>
    <%@ include file="start.jspf" %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/katex/katex.min.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/tutor.css?v=20261028-format">
        <header class="page-heading">
            <div>
                <p class="eyebrow">CÙNG HIỂU VẬT LÝ</p>
                <h1>Trợ lý học tập</h1>
                <p>Đặt câu hỏi, tìm hiểu công thức và luyện cách giải từng bước.</p>
            </div>
        </header>
        <% boolean tutorStorage=Boolean.TRUE.equals(request.getAttribute("tutorStorage")); String
            conversationId=(String)request.getAttribute("conversationId"); List<ConversationDao.Conversation>
            conversations=(List<ConversationDao.Conversation>)request.getAttribute("conversations");
                List<ConversationDao.Message> messages=(List<ConversationDao.Message>)request.getAttribute("messages");
                        int
                        tutorPage=request.getAttribute("tutorPage")==null?1:(Integer)request.getAttribute("tutorPage");
                        if(request.getAttribute("tutorError")!=null){%><div class="notice error" role="alert">
                            <%=ViewSupport.h(request.getAttribute("tutorError"))%>
                        </div>
                        <%}%>
                            <%if(!Boolean.TRUE.equals(request.getAttribute("tutorConfigured"))){%>
                                <div class="notice">AI chưa kết nối hoặc đang tạm dừng. Hội thoại vẫn được lưu; bạn có
                                    thể gửi lại khi quản trị viên hoàn tất cấu hình. Không có câu trả lời AI giả lập.
                                </div>
                                <%}%>
                                    <%if(!tutorStorage){%>
                                        <section class="panel">
                                            <h2>Cần kết nối cơ sở dữ liệu</h2>
                                            <p>Lịch sử hội thoại cần MySQL. Liên hệ quản trị viên để hoàn tất kết nối.
                                            </p>
                                        </section>
                                        <%}else{%>
                                            <div class="tutor-layout">
                                                <aside class="panel tutor-history" aria-label="Lịch sử hội thoại">
                                                    <details class="tutor-history-disclosure" open>
                                                        <summary>Hội thoại của bạn (<%=conversations.size()%>)</summary>
                                                        <form method="post" action="<%=base%>/ai-tutor"><input
                                                                type="hidden" name="csrf"
                                                                value="<%=ViewSupport.csrf(request)%>"><input
                                                                type="hidden" name="action" value="new"><button
                                                                class="button primary" type="submit">+ Hội thoại
                                                                mới</button></form>
                                                        <h2>Hội thoại của bạn</h2>
                                                        <%if(conversations.isEmpty()){%>
                                                            <p class="muted">Chưa có hội thoại. Tạo hội thoại mới để bắt
                                                                đầu.</p>
                                                            <%}for(var conversation:conversations){%>
                                                                <a class="tutor-history-link <%=conversation.id().equals(conversationId)?" selected":""%>" href="<%=base%>/ai-tutor?id=<%=conversation.id()%>&amp;listPage=<%=tutorPage%>"
                                                                                <%=conversation.id().equals(conversationId)?"aria-current=location":""%>><span>
                                                                                        <%=ViewSupport.h(conversation.title())%>
                                                                                    </span><small>
                                                                                        <%=ViewSupport.date(conversation.created())%>
                                                                                    </small></a>
                                                                <%}%>
                                                                    <nav class="actions"
                                                                        aria-label="Phân trang hội thoại">
                                                                        <%if(tutorPage>1){%><a class="button secondary"
                                                                                href="<%=base%>/ai-tutor?listPage=<%=tutorPage-1%>">←
                                                                                Trước</a>
                                                                            <%}if(Boolean.TRUE.equals(request.getAttribute("tutorNextPage"))){%>
                                                                                <a class="button secondary"
                                                                                    href="<%=base%>/ai-tutor?listPage=<%=tutorPage+1%>">Tiếp
                                                                                    →</a>
                                                                                <%}%>
                                                                    </nav>
                                                    </details>
                                                </aside>
                                                <section class="panel tutor-chat" aria-label="Khung hội thoại">
                                                    <%if(conversationId==null){%>
                                                        <div class="tutor-empty"><span class="tutor-mark"
                                                                aria-hidden="true">F = ma</span>
                                                            <h2>Hôm nay bạn muốn hiểu điều gì?</h2>
                                                            <p>Mở lịch sử hội thoại để tạo hoặc chọn cuộc trò chuyện.
                                                            </p>
                                                            <ul>
                                                                <li>Giải thích sự khác nhau giữa vận tốc và gia tốc.
                                                                </li>
                                                                <li>Hướng dẫn giải một bài tập về hợp lực.</li>
                                                                <li>Tóm tắt kiến thức bảo toàn cơ năng.</li>
                                                            </ul>
                                                        </div>
                                                        <%}else{%>
                                                            <div class="tutor-chat-heading">
                                                                <h2>Không gian hỏi đáp</h2>
                                                                <form method="post" action="<%=base%>/ai-tutor"
                                                                    data-confirm="Ẩn hội thoại này khỏi danh sách?">
                                                                    <input type="hidden" name="csrf"
                                                                        value="<%=ViewSupport.csrf(request)%>"><input
                                                                        type="hidden" name="action" value="hide"><input
                                                                        type="hidden" name="id"
                                                                        value="<%=conversationId%>"><button
                                                                        class="button secondary" type="submit">Ẩn hội
                                                                        thoại</button>
                                                                </form>
                                                            </div>
                                                            <div class="tutor-messages" aria-label="Tin nhắn">
                                                                <%if(messages.isEmpty()){%>
                                                                    <p class="muted">Viết câu hỏi đầu tiên. Bạn có thể
                                                                        nhập đề bài và các bước đã thử.</p>
                                                                    <%} for(int i=0;i<messages.size();i++){var
                                                                        message=messages.get(i);boolean
                                                                        mine=message.role().equals("user");%>
                                                                        <article class="tutor-message <%=mine?" from-user":"from-tutor"%>"><strong>
                                                                                <%=mine?"Bạn":"Trợ lý học tập"%>
                                                                            </strong>
                                                                            <%if(message.status().equals("pending")){%>
                                                                                <p role="status">Đang chờ xử lý. Nếu yêu
                                                                                    cầu bị gián đoạn, sau hai phút bạn
                                                                                    có thể tải lại trang và gửi lại.</p>
                                                                                <%}else{%>
                                                                                    <div class="tutor-text"
                                                                                        <%=!mine&&message.status().equals("complete")?"data-rich-answer":""%>
                                                                                        >
                                                                                        <%=ViewSupport.h(message.content())%>
                                                                                    </div>
                                                                                    <%}%>
                                                                                        <small>
                                                                                            <%=ViewSupport.date(message.created())%>
                                                                                        </small>
                                                                                        <%if(!mine&&(message.status().equals("failed")||message.status().equals("pending")&&message.created().isBefore(java.time.Instant.now().minusSeconds(120)))&&i>
                                                                                            0){%>
                                                                                            <form method="post"
                                                                                                action="<%=base%>/ai-tutor"
                                                                                                class="tutor-send">
                                                                                                <input type="hidden"
                                                                                                    name="csrf"
                                                                                                    value="<%=ViewSupport.csrf(request)%>"><input
                                                                                                    type="hidden"
                                                                                                    name="action"
                                                                                                    value="send"><input
                                                                                                    type="hidden"
                                                                                                    name="id"
                                                                                                    value="<%=conversationId%>"><input
                                                                                                    type="hidden"
                                                                                                    name="requestId"
                                                                                                    value="<%=message.requestId()%>"><input
                                                                                                    type="hidden"
                                                                                                    name="message"
                                                                                                    value="<%=ViewSupport.h(messages.get(i-1).content())%>"><button
                                                                                                    class="button secondary"
                                                                                                    type="submit">Gửi
                                                                                                    lại câu hỏi</button>
                                                                                            </form>
                                                                                            <%}%>
                                                                        </article>
                                                                        <%}%>
                                                            </div>
                                                            <form method="post" action="<%=base%>/ai-tutor"
                                                                class="tutor-composer tutor-send" id="tutor-composer">
                                                                <input type="hidden" name="csrf"
                                                                    value="<%=ViewSupport.csrf(request)%>"><input
                                                                    type="hidden" name="action" value="send"><input
                                                                    type="hidden" name="id"
                                                                    value="<%=conversationId%>"><input type="hidden"
                                                                    name="requestId" value="<%=request.getAttribute("requestId")%>">
                                                                <label for="tutor-message">Câu hỏi của
                                                                    bạn</label><textarea id="tutor-message"
                                                                    name="message" rows="4" maxlength="4000" required
                                                                    placeholder="Ví dụ: Vì sao lực và phản lực không triệt tiêu nhau?"></textarea>
                                                                <div class="tutor-compose-actions"><small>Enter để gửi ·
                                                                        Shift+Enter để xuống dòng · tối đa 4.000 ký
                                                                        tự</small><button type="submit"
                                                                        class="button primary">Gửi câu hỏi →</button>
                                                                </div>
                                                                <p class="tutor-loading" role="status" hidden>Đang xử lý
                                                                    yêu cầu, vui lòng giữ trang mở…</p>
                                                                <p class="muted">Chỉ gửi nội dung học tập cần thiết,
                                                                    tránh thông tin cá nhân. Phản hồi AI có thể sai; hãy
                                                                    đối chiếu bài giảng và trao đổi với giáo viên.</p>
                                                            </form>
                                                            <%}%>
                                                </section>
                                            </div>
                                            <%}%>
                                                <script
                                                    src="${pageContext.request.contextPath}/assets/vendor/katex/katex.min.js"
                                                    defer></script>
                                                <script
                                                    src="${pageContext.request.contextPath}/assets/js/tutor-format.js?v=20261028-escaped-lines-2"
                                                    defer></script>
                                                <script
                                                    src="${pageContext.request.contextPath}/assets/js/tutor.js?v=20261030-utf8" charset="UTF-8"
                                                    defer></script>
                                                <%@ include file="end.jspf" %>