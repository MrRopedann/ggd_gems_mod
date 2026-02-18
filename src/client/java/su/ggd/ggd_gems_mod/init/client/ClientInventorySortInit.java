package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.inventory.sort.client.InventorySortButtons;
import su.ggd.ggd_gems_mod.inventory.sort.client.InventorySortClient;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientInventorySortInit {
    private ClientInventorySortInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientInventorySortInit")) return;

        InventorySortClient.init();
        InventorySortButtons.init();
    }
}
