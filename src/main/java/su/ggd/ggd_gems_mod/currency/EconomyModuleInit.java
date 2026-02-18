package su.ggd.ggd_gems_mod.currency;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class EconomyModuleInit {
    private EconomyModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("EconomyModuleInit")) return;

        PiastreDrops.register();
    }
}
