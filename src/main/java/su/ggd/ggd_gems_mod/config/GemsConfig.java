package su.ggd.ggd_gems_mod.config;

import java.util.*;

public class GemsConfig {
    public int maxLevel = 10;
    public List<GemDef> gems = new ArrayList<>();

    // индекс по type (НЕ сериализуем в json)
    public transient Map<String, GemDef> byType = new HashMap<>();

    public void rebuildIndex() {
        byType.clear();
        if (gems == null) return;

        for (GemDef g : gems) {
            if (g == null) continue;
            if (g.type == null || g.type.isBlank()) continue;
            byType.put(g.type.toLowerCase(Locale.ROOT), g);
        }
    }

    public static class GemDef {
        public String type;
        public String name;
        public String description;
        public List<String> targets;
        public String stat;
        public double base;
        public double perLevel;
        public int maxLevel = 10; // у тебя в json есть maxLevel на гем — добавим
    }
}
