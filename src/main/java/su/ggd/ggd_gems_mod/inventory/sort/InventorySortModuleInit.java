package su.ggd.ggd_gems_mod.inventory.sort;

import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortServer;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class InventorySortModuleInit {
    private InventorySortModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("InventorySortModuleInit")) return;

        InventorySortServer.init();
    }
}
