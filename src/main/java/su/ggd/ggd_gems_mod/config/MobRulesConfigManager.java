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

    private static MobRulesConfig loadOrCreate() {
        try {
            Files.createDirectories(PATH.getParent());
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH);
                MobRulesConfig cfg = GSON.fromJson(json, MobRulesConfig.class);
                if (cfg == null) cfg = new MobRulesConfig();
                return cfg;
            }
        } catch (Exception ignored) {
        }

        MobRulesConfig def = new MobRulesConfig();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(def));
        } catch (IOException ignored) {}
        return def;
    }
}
