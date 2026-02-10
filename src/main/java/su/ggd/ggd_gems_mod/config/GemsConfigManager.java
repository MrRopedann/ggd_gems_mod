package su.ggd.ggd_gems_mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class GemsConfigManager {
    private GemsConfigManager() {}

    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("ggd_gems_mod");
    private static final Path FILE = DIR.resolve("gems.json");

    private static GemsConfig config = new GemsConfig();

    public static GemsConfig get() {
        return config;
    }

    public static void loadOrCreateDefault() {
        try {
            Files.createDirectories(DIR);

            if (!Files.exists(FILE)) {
                GemsConfig def = buildDefault();
                normalizeAndReindex(def);
                write(def);
                config = def;
                return;
            }

            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            GemsConfig loaded = GSON.fromJson(json, GemsConfig.class);
            config = (loaded == null) ? new GemsConfig() : loaded;

            boolean changed = normalizeAndReindex(config);
            if (changed) write(config);

        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to load/create gems config: " + FILE);
            e.printStackTrace();

            GemsConfig def = buildDefault();
            normalizeAndReindex(def);
            config = def;

            try { write(def); } catch (IOException ignored) {}
        }
    }

    static boolean normalizeAndReindex(GemsConfig cfg) {
        boolean changed = false;

        if (cfg.gems == null) {
            cfg.gems = new java.util.ArrayList<>();
            changed = true;
        }

        if (cfg.maxLevel <= 0) {
            cfg.maxLevel = 10;
            changed = true;
        }

        // нормализуем maxLevel у каждого gem (если отсутствует/некорректный)
        for (GemsConfig.GemDef g : cfg.gems) {
            if (g == null) continue;
            if (g.maxLevel <= 0) { g.maxLevel = cfg.maxLevel; changed = true; }
            if (g.maxLevel > cfg.maxLevel) { g.maxLevel = cfg.maxLevel; changed = true; }
        }

        cfg.rebuildIndex();
        return changed;
    }

    private static GemsConfig.GemDef gem(
            String type,
            String name,
            String description,
            java.util.List<String> targets,
            String stat,
            double base,
            double perLevel,
            int maxLevel
    ) {
        GemsConfig.GemDef g = new GemsConfig.GemDef();
        g.type = type;
        g.name = name;
        g.description = description;
        g.targets = targets;
        g.stat = stat;
        g.base = base;
        g.perLevel = perLevel;
        g.maxLevel = maxLevel;
        return g;
    }

    private static java.util.List<String> t(String... v) {
        return java.util.Arrays.asList(v);
    }

    private static GemsConfig buildDefault() {
        GemsConfig def = new GemsConfig();
        def.maxLevel = 10;

        def.gems = new java.util.ArrayList<>();
        def.gems.add(gem("atk_damage", "Самоцвет Урона", "Увеличивает урон атаки.",
                t("WEAPON"), "minecraft:attack_damage", 0.5, 0.25, 10));

        def.gems.add(gem("atk_speed", "Самоцвет Скорости", "Увеличивает скорость атаки.",
                t("WEAPON"), "minecraft:attack_speed", 0.05, 0.03, 10));

        def.gems.add(gem("atk_knockback", "Самоцвет Отбрасывания", "Увеличивает силу отбрасывания.",
                t("WEAPON"), "minecraft:attack_knockback", 0.05, 0.03, 10));

        def.gems.add(gem("sweep_ratio", "Самоцвет Размашистости", "Увеличивает урон размашистой атаки.",
                t("WEAPON"), "minecraft:sweeping_damage_ratio", 0.05, 0.03, 10));

        def.gems.add(gem("armor", "Самоцвет Брони", "Увеличивает броню.",
                t("ARMOR"), "minecraft:armor", 1.0, 0.5, 10));

        def.gems.add(gem("kb_resist", "Самоцвет Устойчивости", "Увеличивает сопротивление отбрасыванию.",
                t("ARMOR"), "minecraft:knockback_resistance", 0.02, 0.01, 10));

        def.gems.add(gem("expl_kb_resist", "Самоцвет Взрывоустойчивости", "Увеличивает сопротивление отбрасыванию от взрывов.",
                t("ARMOR"), "minecraft:explosion_knockback_resistance", 0.02, 0.01, 10));

        def.gems.add(gem("max_health", "Самоцвет Здоровья", "Увеличивает максимальное здоровье.",
                t("ARMOR"), "minecraft:max_health", 2.0, 1.0, 10));

        def.gems.add(gem("max_absorption", "Самоцвет Поглощения", "Увеличивает максимальное поглощение.",
                t("ARMOR"), "minecraft:max_absorption", 1.0, 0.5, 10));

        def.gems.add(gem("luck", "Самоцвет Удачи", "Увеличивает удачу.",
                t("ARMOR"), "minecraft:luck", 0.25, 0.15, 10));

        def.gems.add(gem("oxygen", "Самоцвет Дыхания", "Увеличивает запас кислорода.",
                t("ARMOR"), "minecraft:oxygen_bonus", 1.0, 1.0, 10));

        def.gems.add(gem("fall_mult", "Самоцвет Лёгкости Падения", "Снижает урон от падения.",
                t("BOOTS"), "minecraft:fall_damage_multiplier", -0.05, -0.03, 10));

        def.gems.add(gem("move_speed", "Самоцвет Ловкости", "Слегка увеличивает скорость передвижения.",
                t("BOOTS"), "minecraft:movement_speed", 0.005, 0.002, 10));

        def.gems.add(gem("jump", "Самоцвет Прыжка", "Увеличивает силу прыжка.",
                t("BOOTS"), "minecraft:jump_strength", 0.02, 0.01, 10));

        def.gems.add(gem("break_speed", "Самоцвет Горняка", "Увеличивает скорость ломания блоков.",
                t("TOOLS"), "minecraft:block_break_speed", 0.1, 0.05, 10));

        def.gems.add(gem("mining_eff", "Самоцвет Эффективности", "Увеличивает эффективность добычи.",
                t("TOOLS"), "minecraft:mining_efficiency", 0.1, 0.05, 10));

        def.gems.add(gem("submerged_mining", "Самоцвет Подводной Добычи", "Увеличивает скорость добычи под водой.",
                t("TOOLS"), "minecraft:submerged_mining_speed", 0.02, 0.01, 10));

        return def;
    }

    private static void write(GemsConfig cfg) throws IOException {
        String json = GSON.toJson(cfg);
        Files.writeString(
                FILE,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ===== NETWORK SYNC SUPPORT =====

    public static String toJsonString() {
        return GSON.toJson(config);
    }

    public static void applyFromNetworkJson(String json) {
        try {
            GemsConfig loaded = GSON.fromJson(json, GemsConfig.class);
            GemsConfig next = (loaded == null) ? new GemsConfig() : loaded;
            normalizeAndReindex(next);
            config = next;
        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to apply network gems config");
            e.printStackTrace();
        }
    }

    public static String sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
