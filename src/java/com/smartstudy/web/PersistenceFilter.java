package com.smartstudy.web;

import com.smartstudy.persistence.Database;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

/** Ordered before authorization by web.xml. One writer across Tomcat instances. */
public final class PersistenceFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());
        boolean multipart =
                req.getContentType() != null
                        && req.getContentType()
                                .toLowerCase(java.util.Locale.ROOT)
                                .startsWith("multipart/");
        String action = multipart ? null : req.getParameter("action");
        boolean remoteAI =
                path.endsWith("/ai-tutor")
                        || path.endsWith("/ai-questions")
                                && ("generate".equals(action)
                                        || action != null && action.startsWith("regenerate_"))
                        || path.endsWith("/essays") && "ai-review".equals(action)
                        || path.endsWith("/essay-photo") && "scan".equals(action);
        boolean write = !Database.memory() && "POST".equals(req.getMethod()) && !remoteAI;
        CommitResponse buffered = write ? new CommitResponse(resp) : null;
        boolean begun = false;
        Database.requestScope();
        try {
            if (write) {
                Database.begin();
                begun = true;
                Database.use(
                        c -> {
                            try (var s =
                                            c.prepareStatement(
                                                    "SELECT id FROM write_lock WHERE id=1 FOR"
                                                        + " UPDATE");
                                    var r = s.executeQuery()) {
                                if (!r.next())
                                    throw new java.sql.SQLException("Missing migration lock");
                                return null;
                            }
                        });
            }
            chain.doFilter(request, buffered == null ? response : buffered);
            if (begun) {
                begun = false;
                Database.end(buffered.getStatus() < 400);
                buffered.finish();
            }
        } catch (Database.StorageException | java.sql.SQLException e) {
            Throwable diagnostic = e instanceof Database.StorageException ? e.getCause() : e;
            java.util.logging.Logger.getLogger(PersistenceFilter.class.getName())
                    .warning(
                            "JDBC request failed: "
                                    + diagnostic.getClass().getSimpleName()
                                    + "; SQLState="
                                    + (diagnostic instanceof java.sql.SQLException sql
                                            ? sql.getSQLState()
                                            : "unknown"));
            if (begun)
                try {
                    Database.end(false);
                } catch (java.sql.SQLException ignored) {
                }
            if (!resp.isCommitted())
                resp.sendError(503, "Dữ liệu tạm thời không khả dụng. Vui lòng thử lại sau.");
        } catch (IOException | ServletException | RuntimeException | Error e) {
            if (begun)
                try {
                    Database.end(false);
                } catch (java.sql.SQLException ignored) {
                }
            Throwable cause = e;
            for (int depth = 0; cause != null && depth < 12; depth++, cause = cause.getCause()) {
                if (cause instanceof Database.StorageException) {
                    if (!resp.isCommitted())
                        resp.sendError(
                                503, "Dịch vụ dữ liệu tạm thời chưa sẵn sàng. Vui lòng thử lại.");
                    return;
                }
            }
            throw e;
        } finally {
            Database.closeScope();
        }
    }
}
