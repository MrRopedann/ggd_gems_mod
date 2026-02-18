package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;

public final class GemsConfigSyncServer {
    private GemsConfigSyncServer() {}

    public static void sendTo(ServerPlayerEntity player) {
        String json = GemsConfigManager.toJsonString();
        if (json == null) json = "{}";

        String hash = GemsConfigManager.sha256(json);
        ServerPlayNetworking.send(player, new ModNet.SyncGemsConfigPayload(hash, json));
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            sendTo(p);
        }
    }
}
