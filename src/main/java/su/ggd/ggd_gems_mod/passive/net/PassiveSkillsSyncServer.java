package su.ggd.ggd_gems_mod.passive.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.state.PassiveSkillsState;

public final class PassiveSkillsSyncServer {
    private PassiveSkillsSyncServer() {}

    public static void sendConfigTo(ServerPlayerEntity player) {
        String json = PassiveSkillsConfigManager.toJsonString();
        if (json == null) json = "{}";

        String hash = PassiveSkillsConfigManager.sha256(json);
        ServerPlayNetworking.send(player, new PassiveSkillsNet.SyncPassiveSkillsConfigPayload(hash, json));
    }

    public static void sendPlayerSkillsTo(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld sw)) return;

        PassiveSkillsState state = PassiveSkillsState.get(sw);
        String json = (state != null) ? state.toJsonFor(player.getUuid()) : "{}";

        ServerPlayNetworking.send(player, new PassiveSkillsNet.SyncPlayerPassiveSkillsPayload(json));
    }
}
