package su.ggd.ggd_gems_mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
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
                NpcTradersConfig def = buildDefault(); // <-- нужный default
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
            if (t.trades.isEmpty()) {
                t.trades.addAll(defaultTradesForTrader(t.id));
                changed = true;
            }

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

                if (tr.sell == null && tr.sellGem == null) {
                    tr.sellGem = new NpcTradersConfig.SellGemDef();
                    tr.sellGem.type = "atk_damage";
                    tr.sellGem.level = 1;
                    changed = true;
                }

                if (tr.sell != null) {
                    if (tr.sell.item == null) { tr.sell.item = "minecraft:cobblestone"; changed = true; }
                    if (tr.sell.count <= 0) { tr.sell.count = 1; changed = true; }
                }

                if (tr.sellGem != null) {
                    if (tr.sellGem.type == null) { tr.sellGem.type = "atk_damage"; changed = true; }
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

    private static List<NpcTradersConfig.TradeDef> defaultTradesForTrader(String traderId) {
        String id = (traderId == null ? "" : traderId.trim().toLowerCase(Locale.ROOT));
        return switch (id) {
            case "gems" -> buildDefaultGemTradesFixed();
            case "resources" -> buildDefaultResourceTrades();
            default -> java.util.List.of(buildDefaultGemTraderSingleFixedTrade());
        };
    }

    // ---------- DEFAULT = РОВНО КАК В СООБЩЕНИИ ----------

    private static List<NpcTradersConfig.TradeDef> buildDefaultGemTradesFixed() {
        String currency = "ggd_gems_mod:piastre";
        int price = 30;

        // Ровно список из твоего JSON (в указанном порядке)
        String[] gemTypes = new String[] {
                "atk_damage",
                "atk_speed",
                "atk_knockback",
                "sweep_ratio",
                "armor",
                "kb_resist",
                "expl_kb_resist",
                "max_health",
                "max_absorption",
                "luck",
                "oxygen",
                "fall_mult",
                "move_speed",
                "jump",
                "break_speed",
                "mining_eff",
                "submerged_mining"
        };

        var out = new java.util.ArrayList<NpcTradersConfig.TradeDef>(gemTypes.length);
        for (String type : gemTypes) {
            NpcTradersConfig.TradeDef tr = new NpcTradersConfig.TradeDef();

            tr.buy = new NpcTradersConfig.StackDef();
            tr.buy.item = currency;
            tr.buy.count = price;

            tr.sellGem = new NpcTradersConfig.SellGemDef();
            tr.sellGem.type = type;
            tr.sellGem.level = 1;

            tr.maxUses = 999999;
            tr.merchantExperience = 0;
            tr.priceMultiplier = 0.0f;

            out.add(tr);
        }
        return out;
    }

    private static NpcTradersConfig.TradeDef buildDefaultGemTraderSingleFixedTrade() {
        NpcTradersConfig.TradeDef tr = new NpcTradersConfig.TradeDef();
        tr.buy = new NpcTradersConfig.StackDef();
        tr.buy.item = "ggd_gems_mod:piastre";
        tr.buy.count = 30;

        tr.sellGem = new NpcTradersConfig.SellGemDef();
        tr.sellGem.type = "atk_damage";
        tr.sellGem.level = 1;

        tr.maxUses = 999999;
        tr.merchantExperience = 0;
        tr.priceMultiplier = 0.0f;
        return tr;
    }

    private static List<NpcTradersConfig.TradeDef> buildDefaultResourceTrades() {
        var out = new java.util.ArrayList<NpcTradersConfig.TradeDef>();

        out.add(trade("ggd_gems_mod:piastre", 1, "minecraft:cobblestone", 64));
        out.add(trade("ggd_gems_mod:piastre", 1, "minecraft:dirt", 64));
        out.add(trade("ggd_gems_mod:piastre", 2, "minecraft:sand", 64));
        out.add(trade("ggd_gems_mod:piastre", 2, "minecraft:gravel", 64));
        out.add(trade("ggd_gems_mod:piastre", 3, "minecraft:oak_log", 16));
        out.add(trade("ggd_gems_mod:piastre", 3, "minecraft:spruce_log", 16));
        out.add(trade("ggd_gems_mod:piastre", 2, "minecraft:oak_planks", 64));
        out.add(trade("ggd_gems_mod:piastre", 2, "minecraft:coal", 16));
        out.add(trade("ggd_gems_mod:piastre", 2, "minecraft:torch", 32));
        out.add(trade("ggd_gems_mod:piastre", 3, "minecraft:string", 16));
        out.add(trade("ggd_gems_mod:piastre", 3, "minecraft:arrow", 32));
        out.add(trade("ggd_gems_mod:piastre", 2, "minecraft:bread", 8));
        out.add(trade("ggd_gems_mod:piastre", 3, "minecraft:cooked_beef", 8));
        out.add(trade("ggd_gems_mod:piastre", 4, "minecraft:iron_ingot", 4));
        out.add(trade("ggd_gems_mod:piastre", 3, "minecraft:copper_ingot", 8));
        out.add(trade("ggd_gems_mod:piastre", 4, "minecraft:redstone", 16));
        out.add(trade("ggd_gems_mod:piastre", 4, "minecraft:lapis_lazuli", 16));

        return out;
    }

    private static NpcTradersConfig buildDefault() {
        NpcTradersConfig cfg = new NpcTradersConfig();

        // gems
        NpcTradersConfig.TraderDef gems = new NpcTradersConfig.TraderDef();
        gems.id = "gems";
        gems.name = "Торговец самоцветами";
        gems.entityType = "minecraft:villager";
        gems.profession = "minecraft:cleric";
        gems.spawnDistance = 2.0;
        gems.invulnerable = true;
        gems.noAi = true;
        gems.nameVisible = true;
        gems.trades.addAll(buildDefaultGemTradesFixed());
        cfg.traders.add(gems);

        // resources
        NpcTradersConfig.TraderDef res = new NpcTradersConfig.TraderDef();
        res.id = "resources";
        res.name = "Скупщик ресурсов";
        res.entityType = "minecraft:villager";
        res.profession = "minecraft:toolsmith";
        res.spawnDistance = 2.0;
        res.invulnerable = true;
        res.noAi = true;
        res.nameVisible = true;
        res.trades.addAll(buildDefaultResourceTrades());
        cfg.traders.add(res);

        cfg.rebuildIndex();
        return cfg;
    }

    private static NpcTradersConfig.TradeDef trade(String buyItem, int buyCount, String sellItem, int sellCount) {
        NpcTradersConfig.TradeDef tr = new NpcTradersConfig.TradeDef();

        tr.buy = new NpcTradersConfig.StackDef();
        tr.buy.item = buyItem;
        tr.buy.count = buyCount;

        tr.sell = new NpcTradersConfig.StackDef();
        tr.sell.item = sellItem;
        tr.sell.count = sellCount;

        tr.maxUses = 999999;
        tr.merchantExperience = 0;
        tr.priceMultiplier = 0.0f;

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
