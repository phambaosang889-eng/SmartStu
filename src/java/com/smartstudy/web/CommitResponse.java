package com.smartstudy.web;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.*;
import java.io.*;

/** Do not acknowledge a write to the browser before its JDBC commit succeeds. */
final class CommitResponse extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private PrintWriter writer;
    private int status = 200;
    private String error;
    private boolean sendError;

    CommitResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int code) {
        status = code;
        super.setStatus(code);
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void sendRedirect(String location) {
        resetBuffer();
        setStatus(302);
        setHeader("Location", location);
    }

    @Override
    public void sendError(int code, String message) {
        resetBuffer();
        setStatus(code);
        sendError = true;
        error = message;
    }

    @Override
    public void sendError(int code) {
        sendError(code, null);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStream() {
            @Override
            public void write(int value) throws IOException {
                if (buffer.size() >= 2_000_000) throw new IOException("Response too large");
                buffer.write(value);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {
                throw new UnsupportedOperationException("Synchronous MVC response");
            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null)
            writer =
                    new PrintWriter(
                            new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
        return writer;
    }

    @Override
    public void flushBuffer() {
        if (writer != null) writer.flush();
    }

    @Override
    public void resetBuffer() {
        if (writer != null) writer.flush();
        buffer.reset();
    }

    void finish() throws IOException {
        flushBuffer();
        HttpServletResponse response = (HttpServletResponse) getResponse();
        if (sendError) {
            response.sendError(status, error);
            return;
        }
        response.setStatus(status);
        buffer.writeTo(response.getOutputStream());
        response.flushBuffer();
    }
}
