package su.ggd.ggd_gems_mod.passive.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import su.ggd.ggd_gems_mod.passive.PassiveSkillService;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class PassiveSkillsUpgradeServer {
    private PassiveSkillsUpgradeServer() {}

    public static void init() {
        if (!InitOnce.markDone("PassiveSkillsUpgradeServer")) return;

        ServerPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player() == null) return;
                PassiveSkillService.tryUpgrade(context.player(), payload.skillId());
            });
        });
    }
}
