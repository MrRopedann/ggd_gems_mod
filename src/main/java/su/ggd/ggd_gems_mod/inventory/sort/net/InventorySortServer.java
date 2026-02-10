package su.ggd.ggd_gems_mod.inventory.sort.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import su.ggd.ggd_gems_mod.inventory.sort.InventorySortType;
import su.ggd.ggd_gems_mod.inventory.sort.InventorySorter;

public final class InventorySortServer {
    private InventorySortServer() {}

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(InventorySortNet.SORT_REQUEST_ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player() == null) return;
                InventorySortType type = InventorySortType.fromWire(payload.sortType());
                InventorySorter.sortCurrentScreen(context.player(), type);
            });
        });
    }
}
