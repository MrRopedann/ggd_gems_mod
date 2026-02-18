package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class GemsConfigSyncServer {
    private GemsConfigSyncServer() {}

    public static void init() {
        if (!InitOnce.markDone("GemsConfigSyncServer")) return;

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendTo(handler.player));
    }

    public static void sendTo(ServerPlayerEntity player) {
        // НИКАКИХ loadOrCreateDefault() здесь.
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
