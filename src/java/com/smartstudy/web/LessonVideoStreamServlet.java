package com.smartstudy.web;

import author.User;
import com.smartstudy.service.LessonVideoService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;

/** Authenticated range streaming; no physical path or upload filename is exposed. */
@WebServlet("/media/lesson-video")
public final class LessonVideoStreamServlet extends HttpServlet {
    private final LessonVideoService videos = new LessonVideoService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        serve(req, resp, false);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        serve(req, resp, true);
    }

    private void serve(HttpServletRequest req, HttpServletResponse resp, boolean head)
            throws IOException {
        User user = (User) req.getSession().getAttribute("user");
        var video = videos.readable(user, req.getParameter("id"));
        if (video == null || !video.type().equals("upload")) {
            resp.sendError(404);
            return;
        }
        Path path = videos.file(video);
        if (!Files.isRegularFile(path)) {
            resp.sendError(404);
            return;
        }
        long length = Files.size(path), start = 0, end = length - 1;
        String range = head ? null : req.getHeader("Range");
        if (range != null) {
            try {
                if (!range.matches("bytes=\\d*-\\d*")) throw new IllegalArgumentException();
                String[] bounds = range.substring(6).split("-", -1);
                if (bounds[0].isEmpty()) {
                    long suffix = Long.parseLong(bounds[1]);
                    if (suffix <= 0) throw new IllegalArgumentException();
                    start = Math.max(0, length - suffix);
                } else {
                    start = Long.parseLong(bounds[0]);
                    if (!bounds[1].isEmpty()) end = Math.min(end, Long.parseLong(bounds[1]));
                }
                if (start >= length || start > end) throw new IllegalArgumentException();
            } catch (RuntimeException e) {
                resp.setHeader("Content-Range", "bytes */" + length);
                resp.setStatus(416);
                return;
            }
            resp.setStatus(206);
            resp.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
        }
        resp.setHeader("Cache-Control", "private, no-store");
        resp.setHeader("Accept-Ranges", "bytes");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setContentType(video.mime());
        resp.setContentLengthLong(end - start + 1);
        if (head) return;
        try (var file = new RandomAccessFile(path.toFile(), "r")) {
            file.seek(start);
            long remaining = end - start + 1;
            byte[] buffer = new byte[65536];
            var out = resp.getOutputStream();
            while (remaining > 0) {
                int count = file.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (count < 0) break;
                out.write(buffer, 0, count);
                remaining -= count;
            }
        }
    }
}
