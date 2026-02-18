package su.ggd.ggd_gems_mod.mob;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class MobModuleInit {
    private MobModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("MobModuleInit")) return;

        DayHostileSpawner.init();
        MobLevels.init();
    }
}
