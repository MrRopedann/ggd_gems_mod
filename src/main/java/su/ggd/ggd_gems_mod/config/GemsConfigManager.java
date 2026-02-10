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

    private static GemsConfig buildDefault() {
        GemsConfig def = new GemsConfig();
        def.maxLevel = 10;
        // можно оставить пустым или добавить 1-2 дефолтных гема
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
