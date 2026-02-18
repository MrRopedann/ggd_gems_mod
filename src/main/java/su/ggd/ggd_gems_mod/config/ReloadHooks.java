package su.ggd.ggd_gems_mod.config;

import java.util.ArrayList;
import java.util.List;

public final class ReloadHooks {

    private static final List<Runnable> HOOKS = new ArrayList<>();
    private static boolean FROZEN = false;

    public static void register(Runnable hook) {
        if (hook == null) throw new IllegalArgumentException("hook == null");
        if (FROZEN) throw new IllegalStateException("ReloadHooks is frozen");
        HOOKS.add(hook);
    }

    /** Вызывать после регистрации всех хуков (обычно в onInitialize). */
    public static void freeze() {
        FROZEN = true;
    }

    /** Запускается после ConfigRegistry.loadAll(). Один модуль не должен валить reload целиком. */
    public static void run() {
        for (Runnable r : HOOKS) {
            try {
                r.run();
            } catch (Exception e) {
                System.err.println("[ggd_gems_mod] Reload hook failed: " + r);
                e.printStackTrace();
            }
        }
    }

    private ReloadHooks() {}
}
