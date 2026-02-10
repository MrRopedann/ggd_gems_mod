package su.ggd.ggd_gems_mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class EconomyConfigManager {
    private EconomyConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("ggd_gems_mod");
    private static final Path FILE = DIR.resolve("economy.json");

    private static EconomyConfig config = new EconomyConfig();

    public static EconomyConfig get() {
        return config;
    }

    public static void loadOrCreateDefault() {
        try {
            Files.createDirectories(DIR);

            if (!Files.exists(FILE)) {
                EconomyConfig def = new EconomyConfig();
                normalize(def);
                write(def);
                config = def;
                return;
            }

            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            EconomyConfig loaded = GSON.fromJson(json, EconomyConfig.class);
            config = (loaded == null) ? new EconomyConfig() : loaded;

            boolean changed = normalize(config);
            if (changed) write(config);

        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to load/create economy config: " + FILE);
            e.printStackTrace();

            EconomyConfig def = new EconomyConfig();
            normalize(def);
            config = def;

            try { write(def); } catch (IOException ignored) {}
        }
    }

    private static boolean normalize(EconomyConfig cfg) {
        boolean changed = false;

        if (cfg.currency == null) { cfg.currency = new EconomyConfig.Currency(); changed = true; }
        if (cfg.currency.itemId == null || cfg.currency.itemId.isBlank()) { cfg.currency.itemId = "ggd_gems_mod:piastre"; changed = true; }

        if (cfg.anvil == null) { cfg.anvil = new EconomyConfig.AnvilCosts(); changed = true; }
        if (cfg.anvil.insertBase <= 0) { cfg.anvil.insertBase = 5; changed = true; }
        if (cfg.anvil.combineBase <= 0) { cfg.anvil.combineBase = 8; changed = true; }
        if (cfg.anvil.insertPerLevelPct < 0) { cfg.anvil.insertPerLevelPct = 0.20; changed = true; }
        if (cfg.anvil.combinePerLevelPct < 0) { cfg.anvil.combinePerLevelPct = 0.20; changed = true; }

        if (cfg.drops == null) { cfg.drops = new EconomyConfig.Drops(); changed = true; }
        if (cfg.drops.chance < 0) { cfg.drops.chance = 0.15; changed = true; }
        if (cfg.drops.min < 0) { cfg.drops.min = 1; changed = true; }
        if (cfg.drops.max < cfg.drops.min) { cfg.drops.max = Math.max(cfg.drops.min, 2); changed = true; }

        return changed;
    }

    private static void write(EconomyConfig cfg) throws IOException {
        String json = GSON.toJson(cfg);
        Files.writeString(
                FILE,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // если позже захочешь синкать economy.json клиенту — эти методы уже готовы
    public static String toJsonString() { return GSON.toJson(config); }

    public static void applyFromNetworkJson(String json) {
        try {
            EconomyConfig loaded = GSON.fromJson(json, EconomyConfig.class);
            EconomyConfig next = (loaded == null) ? new EconomyConfig() : loaded;
            normalize(next);
            config = next;
        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to apply network economy config");
            e.printStackTrace();
        }
    }
}
