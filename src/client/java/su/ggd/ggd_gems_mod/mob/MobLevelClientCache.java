package su.ggd.ggd_gems_mod.mob;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public final class MobLevelClientCache {
    private MobLevelClientCache() {}

    private static final Int2IntOpenHashMap LEVELS = new Int2IntOpenHashMap();
    static {
        LEVELS.defaultReturnValue(1);
    }

    public static void set(int entityId, int level) {
        LEVELS.put(entityId, Math.max(1, level));
    }

    public static int get(int entityId) {
        return LEVELS.get(entityId);
    }
}
