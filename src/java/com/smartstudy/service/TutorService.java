package com.smartstudy.service;

import author.User;
import com.smartstudy.dao.ConversationDao;
import java.util.*;

public final class TutorService {
    private final ConversationDao dao = new ConversationDao();

    public List<ConversationDao.Conversation> conversations(User u) {
        return dao.list(u);
    }

    public List<ConversationDao.Conversation> conversations(User u, int page) {
        return dao.list(u, page);
    }

    public List<ConversationDao.Message> messages(User u, String id) {
        return dao.messages(u, id);
    }

    public String create(User u) {
        return dao.create(u);
    }

    public void hide(User u, String id) {
        dao.hide(u, id);
    }

    public void send(User u, String id, String token, String text) {
        try {
            if (!UUID.fromString(token).toString().equals(token))
                throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalArgumentException("Mã gửi tin không hợp lệ. Tải lại trang.");
        }
        text = CatalogService.required(text, 4000, "Câu hỏi");
        if (!dao.claim(u, id, token, text)) return;
        try {
            var history =
                    dao.messages(u, id).stream()
                            .filter(m -> m.status().equals("complete"))
                            .toList();
            List<Map<String, String>> turns =
                    new ArrayList<>(
                            history.stream()
                                    .skip(Math.max(0, history.size() - 12))
                                    .map(m -> Map.of("role", m.role(), "content", m.content()))
                                    .toList());
            while (turns.size() > 1
                    && turns.stream().mapToInt(turn -> turn.get("content").length()).sum() > 16000)
                turns.remove(0);
            while (turns.size() > 1 && "assistant".equals(turns.get(0).get("role")))
                turns.remove(0);
            String answer = AIService.getInstance().tutor(u, turns);
            dao.finish(u, id, token, answer, true);
        } catch (IllegalArgumentException e) {
            dao.finish(u, id, token, e.getMessage(), false);
        }
    }
}
