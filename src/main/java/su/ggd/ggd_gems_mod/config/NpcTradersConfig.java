package su.ggd.ggd_gems_mod.config;

import java.util.*;
import java.util.Locale;

public class NpcTradersConfig {

    public List<TraderDef> traders = new ArrayList<>();

    // runtime index (not serialized)
    public transient Map<String, TraderDef> byId = new HashMap<>();

    public void rebuildIndex() {
        byId.clear();
        if (traders == null) return;

        for (TraderDef t : traders) {
            if (t == null) continue;
            if (t.id == null || t.id.isBlank()) continue;
            byId.put(t.id.trim().toLowerCase(Locale.ROOT), t);
        }
    }

    public static class TraderDef {
        public String id = "gems";
        public String name = "Торговец";
        public String entityType = "minecraft:villager";
        public String profession = "minecraft:cleric";

        public double spawnDistance = 2.0;

        public boolean invulnerable = true;
        public boolean noAi = true;
        public boolean nameVisible = true;

        public List<TradeDef> trades = new ArrayList<>();
    }

    public static class TradeDef {
        public StackDef buy;          // обязательно

        public StackDef sell;         // либо sell...
        public SellGemDef sellGem;    // ...либо sellGem

        public int maxUses = 999999;
        public int merchantExperience = 0;
        public float priceMultiplier = 0.0f;
    }

    public static class StackDef {
        public String item;  // "minecraft:coal" / "ggd_gems_mod:piastre"
        public int count = 1;
    }

    public static class SellGemDef {
        public String type;  // gem_type из gems.json
        public int level = 1;
    }
}
