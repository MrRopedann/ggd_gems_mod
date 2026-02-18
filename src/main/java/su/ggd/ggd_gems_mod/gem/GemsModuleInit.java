package su.ggd.ggd_gems_mod.gem;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class GemsModuleInit {
    private GemsModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("GemsModuleInit")) return;

        // сейчас пусто; всё завязано на config reload hook и anvil logic
    }
}
