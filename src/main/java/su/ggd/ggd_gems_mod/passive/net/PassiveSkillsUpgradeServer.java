package su.ggd.ggd_gems_mod.passive.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.passive.PassiveSkillService;

public final class PassiveSkillsUpgradeServer {
    private PassiveSkillsUpgradeServer() {}

    public static void handleUpgrade(PassiveSkillsNet.UpgradePassiveSkillPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity player = ctx.player();
        if (player == null) return;

        ctx.server().execute(() -> PassiveSkillService.tryUpgrade(player, payload.skillId()));
    }
}
