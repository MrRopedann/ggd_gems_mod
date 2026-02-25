package su.ggd.ggd_gems_mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MobRulesConfigManager {
    private MobRulesConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of("config", "ggd_gems_mod", "mob_rules.json");

    private static MobRulesConfig CURRENT;

    public static MobRulesConfig get() {
        if (CURRENT == null) CURRENT = loadOrCreate();
        return CURRENT;
    }

    /** Для ConfigRegistry / reload команд. */
    public static void loadOrCreateDefault() {
        CURRENT = null;
        get();
    }

    private static MobRulesConfig loadOrCreate() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH);
                MobRulesConfig cfg = GSON.fromJson(json, MobRulesConfig.class);
                if (cfg == null) cfg = new MobRulesConfig();
                return sanitize(cfg);
            }
        } catch (Exception ignored) {
        }

        MobRulesConfig def = sanitize(new MobRulesConfig());
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(def));
        } catch (IOException ignored) {}
        return def;
    }

    private static MobRulesConfig sanitize(MobRulesConfig cfg) {
        if (cfg == null) cfg = new MobRulesConfig();

        if (cfg.daytimeSpawnIntervalTicks < 1) cfg.daytimeSpawnIntervalTicks = 1;
        if (cfg.daytimeSpawnAttempts < 1) cfg.daytimeSpawnAttempts = 1;

        if (!Double.isFinite(cfg.monsterCapMultiplier) || cfg.monsterCapMultiplier <= 0.0) {
            cfg.monsterCapMultiplier = 1.0;
        }
        if (cfg.monsterCapOverride < 0) cfg.monsterCapOverride = 0;

        if (cfg.daytimeMaxLight < 0) cfg.daytimeMaxLight = 0;
        if (cfg.daytimeMaxLight > 15) cfg.daytimeMaxLight = 15;

        if (!Double.isFinite(cfg.daytimeChanceMinAtLight15)) cfg.daytimeChanceMinAtLight15 = 0.25;
        if (!Double.isFinite(cfg.daytimeChanceMaxAtLight0)) cfg.daytimeChanceMaxAtLight0 = 1.0;
        if (!Double.isFinite(cfg.daytimeChanceCurvePower)) cfg.daytimeChanceCurvePower = 2.0;

        cfg.daytimeChanceMinAtLight15 = clamp01(cfg.daytimeChanceMinAtLight15);
        cfg.daytimeChanceMaxAtLight0 = clamp01(cfg.daytimeChanceMaxAtLight0);

        if (cfg.daytimeChanceCurvePower < 0.1) cfg.daytimeChanceCurvePower = 0.1;
        if (cfg.daytimeChanceCurvePower > 10.0) cfg.daytimeChanceCurvePower = 10.0;

        return cfg;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}