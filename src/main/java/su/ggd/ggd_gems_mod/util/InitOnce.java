package su.ggd.ggd_gems_mod.util;

import java.util.concurrent.ConcurrentHashMap;

public final class InitOnce {
    private InitOnce() {}

    private static final ConcurrentHashMap<String, Boolean> DONE = new ConcurrentHashMap<>();

    /**
     * @return true если ключ был отмечен впервые, false если уже был отмечен раньше
     */
    public static boolean markDone(String key) {
        return DONE.putIfAbsent(key, Boolean.TRUE) == null;
    }
}
