package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;

public final class GemsConfigSyncClient {
    private GemsConfigSyncClient() {}

    private static String lastHash;

    public static void handle(ModNet.SyncGemsConfigPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (payload.hash() != null && payload.hash().equals(lastHash)) return;
            lastHash = payload.hash();

            GemsConfigManager.applyFromNetworkJson(payload.json());

            if (context.client().player != null) {
                context.client().player.sendMessage(Text.literal("§a[Gems] Config synced from server."), true);
            }
        });
    }
}
