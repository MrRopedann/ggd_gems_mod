package su.ggd.ggd_gems_mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;

public final class NpcTradersConfigManager {
    private NpcTradersConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("ggd_gems_mod");
    private static final Path FILE = DIR.resolve("npc_traders.json");

    private static NpcTradersConfig config = new NpcTradersConfig();

    public static NpcTradersConfig get() {
        return config;
    }

    public static void loadOrCreateDefault() {
        try {
            Files.createDirectories(DIR);

            if (!Files.exists(FILE)) {
                NpcTradersConfig def = buildDefault();
                normalize(def);
                write(def);
                config = def;
                return;
            }

            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            NpcTradersConfig loaded = GSON.fromJson(json, NpcTradersConfig.class);
            config = (loaded == null) ? new NpcTradersConfig() : loaded;

            boolean changed = normalize(config);
            config.rebuildIndex();
            if (changed) write(config);

        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to load npc_traders.json");
            e.printStackTrace();

            NpcTradersConfig def = buildDefault();
            normalize(def);
            def.rebuildIndex();
            config = def;

            try { write(def); } catch (IOException ignored) {}
        }
    }

    private static void write(NpcTradersConfig cfg) throws IOException {
        String json = GSON.toJson(cfg);
        Files.writeString(
                FILE,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static boolean normalize(NpcTradersConfig cfg) {
        boolean changed = false;

        if (cfg.traders == null) {
            cfg.traders = new java.util.ArrayList<>();
            changed = true;
        }

        if (cfg.traders.isEmpty()) {
            cfg.traders.addAll(buildDefault().traders);
            changed = true;
        }

        for (NpcTradersConfig.TraderDef t : cfg.traders) {
            if (t == null) continue;

            if (t.id == null || t.id.isBlank()) { t.id = "trader"; changed = true; }
            t.id = t.id.trim().toLowerCase(Locale.ROOT);

            if (t.name == null || t.name.isBlank()) { t.name = "Торговец"; changed = true; }
            if (t.entityType == null || t.entityType.isBlank()) { t.entityType = "minecraft:villager"; changed = true; }
            if (t.profession == null || t.profession.isBlank()) { t.profession = "minecraft:cleric"; changed = true; }

            if (t.spawnDistance <= 0) { t.spawnDistance = 2.0; changed = true; }

            if (t.trades == null) { t.trades = new java.util.ArrayList<>(); changed = true; }
            if (t.trades.isEmpty()) { t.trades.addAll(buildDefaultGemTrader().trades); changed = true; }

            for (NpcTradersConfig.TradeDef tr : t.trades) {
                if (tr == null) continue;

                if (tr.buy == null) {
                    tr.buy = new NpcTradersConfig.StackDef();
                    tr.buy.item = "ggd_gems_mod:piastre";
                    tr.buy.count = 30;
                    changed = true;
                }
                if (tr.buy.item == null) { tr.buy.item = "ggd_gems_mod:piastre"; changed = true; }
                if (tr.buy.count <= 0) { tr.buy.count = 1; changed = true; }

                // sell или sellGem обязателен
                if (tr.sell == null && tr.sellGem == null) {
                    tr.sellGem = new NpcTradersConfig.SellGemDef();
                    tr.sellGem.type = "atk";
                    tr.sellGem.level = 1;
                    changed = true;
                }

                if (tr.sell != null) {
                    if (tr.sell.item == null) { tr.sell.item = "minecraft:cobblestone"; changed = true; }
                    if (tr.sell.count <= 0) { tr.sell.count = 1; changed = true; }
                }

                if (tr.sellGem != null) {
                    if (tr.sellGem.type == null) { tr.sellGem.type = "atk"; changed = true; }
                    tr.sellGem.type = tr.sellGem.type.trim().toLowerCase(Locale.ROOT);
                    if (tr.sellGem.level <= 0) { tr.sellGem.level = 1; changed = true; }
                }

                if (tr.maxUses <= 0) { tr.maxUses = 999999; changed = true; }
                if (tr.merchantExperience < 0) { tr.merchantExperience = 0; changed = true; }
                if (tr.priceMultiplier < 0) { tr.priceMultiplier = 0.0f; changed = true; }
            }
        }

        cfg.rebuildIndex();
        return changed;
    }

    private static NpcTradersConfig buildDefault() {
        NpcTradersConfig cfg = new NpcTradersConfig();
        cfg.traders.add(buildDefaultGemTrader());

        // пример второго торговца ресурсами
        NpcTradersConfig.TraderDef res = new NpcTradersConfig.TraderDef();
        res.id = "resources";
        res.name = "Скупщик ресурсов";
        res.entityType = "minecraft:villager";
        res.profession = "minecraft:toolsmith";

        res.trades.add(trade("ggd_gems_mod:piastre", 2, "minecraft:coal", 8));
        res.trades.add(trade("ggd_gems_mod:piastre", 1, "minecraft:cobblestone", 64));
        res.trades.add(trade("ggd_gems_mod:piastre", 3, "minecraft:oak_log", 16));

        cfg.traders.add(res);

        cfg.rebuildIndex();
        return cfg;
    }

    private static NpcTradersConfig.TraderDef buildDefaultGemTrader() {
        NpcTradersConfig.TraderDef t = new NpcTradersConfig.TraderDef();
        t.id = "gems";
        t.name = "Торговец самоцветами";
        t.entityType = "minecraft:villager";
        t.profession = "minecraft:cleric";

        // по умолчанию: 30 пиастр -> случайный самоцвет 1 лвл
        NpcTradersConfig.TradeDef tr = new NpcTradersConfig.TradeDef();
        tr.buy = new NpcTradersConfig.StackDef();
        tr.buy.item = "ggd_gems_mod:piastre";
        tr.buy.count = 30;

        tr.sellGem = new NpcTradersConfig.SellGemDef();
        tr.sellGem.type = ""; // пусто => будет выбран случайный из gems.json
        tr.sellGem.level = 1;

        t.trades.add(tr);
        return t;
    }

    private static NpcTradersConfig.TradeDef trade(String buyItem, int buyCount, String sellItem, int sellCount) {
        NpcTradersConfig.TradeDef tr = new NpcTradersConfig.TradeDef();

        tr.buy = new NpcTradersConfig.StackDef();
        tr.buy.item = buyItem;
        tr.buy.count = buyCount;

        tr.sell = new NpcTradersConfig.StackDef();
        tr.sell.item = sellItem;
        tr.sell.count = sellCount;

        return tr;
    }

    public static boolean isNpcName(String name) {
        NpcTradersConfig cfg = get();
        if (cfg == null || cfg.traders == null) return false;
        for (NpcTradersConfig.TraderDef t : cfg.traders) {
            if (t != null && t.name != null && t.name.equals(name)) return true;
        }
        return false;
    }

}
