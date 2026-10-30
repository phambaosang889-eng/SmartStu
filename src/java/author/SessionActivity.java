package author;

import java.time.*;
import java.util.*;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.*;

@WebListener
public final class SessionActivity implements HttpSessionListener, HttpSessionIdListener {
    private record Presence(String email, Instant lastSeen) {}

    private static final Map<String, Presence> LIVE = new HashMap<>();

    public static synchronized void touch(String id, String email) {
        LIVE.put(id, new Presence(email, Instant.now()));
    }

    public static synchronized long activeUsers() {
        Instant cutoff = Instant.now().minusSeconds(900);
        LIVE.values().removeIf(p -> p.lastSeen().isBefore(cutoff));
        return LIVE.values().stream()
                .map(Presence::email)
                .distinct()
                .filter(email -> UserStore.active(email) != null)
                .count();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent e) {
        synchronized (SessionActivity.class) {
            LIVE.remove(e.getSession().getId());
        }
    }

    @Override
    public void sessionIdChanged(HttpSessionEvent e, String old) {
        synchronized (SessionActivity.class) {
            Presence p = LIVE.remove(old);
            if (p != null) LIVE.put(e.getSession().getId(), p);
        }
    }
}
