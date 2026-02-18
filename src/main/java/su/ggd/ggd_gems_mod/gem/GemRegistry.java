package su.ggd.ggd_gems_mod.gem;

import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GemRegistry {

    private static Map<String, GemsConfig.GemDef> BY_ID = Map.of();

    public static void rebuild() {
        GemsConfig cfg = GemsConfigManager.get();
        if (cfg == null) {
            BY_ID = Map.of();
            return;
        }

        // на всякий случай: если менеджер не гарантирует rebuildIndex()
        if (cfg.byType == null || cfg.byType.isEmpty()) {
            try {
                cfg.rebuildIndex();
            } catch (Exception ignored) {}
        }

        if (cfg.byType == null || cfg.byType.isEmpty()) {
            BY_ID = Map.of();
            return;
        }

        HashMap<String, GemsConfig.GemDef> map = new HashMap<>();
        for (var e : cfg.byType.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            map.put(e.getKey().trim().toLowerCase(Locale.ROOT), e.getValue());
        }

        BY_ID = map;
    }

    public static GemsConfig.GemDef get(String id) {
        if (id == null) return null;
        return BY_ID.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public static Map<String, GemsConfig.GemDef> all() {
        return Collections.unmodifiableMap(BY_ID);
    }

    private GemRegistry() {}
}
