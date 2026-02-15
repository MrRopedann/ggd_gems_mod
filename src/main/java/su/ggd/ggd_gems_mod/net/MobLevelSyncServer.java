package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MobLevelSyncServer {
    private MobLevelSyncServer() {}

    public static void send(Entity entity, int level) {
        if (entity == null) return;

        int lvl = Math.max(1, level);
        var payload = new MobLevelNet.MobLevelPayload(entity.getId(), lvl);

        for (var player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendTo(ServerPlayerEntity player, Entity entity, int level) {
        if (player == null || entity == null) return;

        int lvl = Math.max(1, level);
        ServerPlayNetworking.send(player, new MobLevelNet.MobLevelPayload(entity.getId(), lvl));
    }
}
