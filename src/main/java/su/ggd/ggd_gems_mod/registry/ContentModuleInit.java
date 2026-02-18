package su.ggd.ggd_gems_mod.registry;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ContentModuleInit {
    private ContentModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("ContentModuleInit")) return;

        ModDataComponents.initialize();
        ModItems.initialize();
        ModItemGroups.initialize();
    }
}
