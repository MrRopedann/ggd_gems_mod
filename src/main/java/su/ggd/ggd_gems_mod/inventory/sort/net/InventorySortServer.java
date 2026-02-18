package su.ggd.ggd_gems_mod.inventory.sort.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.inventory.sort.InventorySortType;
import su.ggd.ggd_gems_mod.inventory.sort.InventorySorter;

public final class InventorySortServer {
    private InventorySortServer() {}

    public static void handleSortRequest(InventorySortNet.SortRequestPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity player = ctx.player();
        if (player == null) return;

        // sortType() приходит "по проводу", переводим в enum как раньше
        InventorySortType type = InventorySortType.fromWire(payload.sortType());

        ctx.server().execute(() -> InventorySorter.sortCurrentScreen(player, type));
    }
}
