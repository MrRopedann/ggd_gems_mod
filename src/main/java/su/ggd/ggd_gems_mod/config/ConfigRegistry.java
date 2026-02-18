package su.ggd.ggd_gems_mod.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigRegistry {

    private static final List<ReloadableConfig> CONFIGS = new ArrayList<>();
    private static boolean FROZEN = false;

    public static void register(ReloadableConfig cfg) {
        if (cfg == null) throw new IllegalArgumentException("cfg == null");
        if (FROZEN) throw new IllegalStateException("ConfigRegistry is frozen");
        CONFIGS.add(cfg);
    }

    /** Вызывать в onInitialize() после регистрации всех конфигов, чтобы больше никто не мог добавить новые. */
    public static void freeze() {
        FROZEN = true;
    }

    public static List<ReloadableConfig> all() {
        return Collections.unmodifiableList(CONFIGS);
    }

    /**
     * Перезагрузка всех зарегистрированных конфигов + пост-хуки (GemRegistry.rebuild и т.п.).
     * Важно: ReloadHooks.run() должен выполняться ПОСЛЕ всех load().
     */
    public static void loadAll() {
        for (ReloadableConfig c : CONFIGS) {
            try {
                c.load();
            } catch (Exception e) {
                System.err.println("[ggd_gems_mod] Failed to load config: " + c.getClass().getName());
                e.printStackTrace();
            }
        }
        ReloadHooks.run();
    }

    private ConfigRegistry() {}
}
