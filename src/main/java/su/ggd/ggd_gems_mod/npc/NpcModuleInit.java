package su.ggd.ggd_gems_mod.npc;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class NpcModuleInit {
    private NpcModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("NpcModuleInit")) return;

        NpcDamageBlocker.init();
    }
}
