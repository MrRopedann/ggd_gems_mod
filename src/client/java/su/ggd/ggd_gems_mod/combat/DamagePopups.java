package su.ggd.ggd_gems_mod.combat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class DamagePopups {
    private DamagePopups() {}

    public static final class Popup {
        public final float amount;
        public final long startMs;

        public Popup(float amount, long startMs) {
            this.amount = amount;
            this.startMs = startMs;
        }
    }

    private static final Map<Integer, Deque<Popup>> POPUPS = new HashMap<>();

    public static void add(int entityId, float amount) {
        if (amount <= 0f) return;
        Deque<Popup> q = POPUPS.computeIfAbsent(entityId, k -> new ArrayDeque<>());
        q.addFirst(new Popup(amount, System.currentTimeMillis()));
        while (q.size() > 5) q.removeLast();
    }

    public static Deque<Popup> get(int entityId) {
        return POPUPS.get(entityId);
    }

    public static void cleanup(int entityId, long now, long lifeMs) {
        Deque<Popup> q = POPUPS.get(entityId);
        if (q == null) return;
        while (!q.isEmpty() && (now - q.peekLast().startMs) > lifeMs) q.removeLast();
        if (q.isEmpty()) POPUPS.remove(entityId);
    }
}
