package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.passive.ClientPassiveSkills;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;

public final class PassiveSkillsSyncClient {
    private PassiveSkillsSyncClient() {}

    private static String lastHash;

    public static void handleConfig(PassiveSkillsNet.SyncPassiveSkillsConfigPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (payload.hash() != null && payload.hash().equals(lastHash)) return;
            lastHash = payload.hash();

            PassiveSkillsConfigManager.applyFromNetworkJson(payload.json());

            if (context.client().player != null) {
                context.client().player.sendMessage(Text.literal("§a[Passives] Config synced from server."), true);
            }
        });
    }

    public static void handlePlayerLevels(PassiveSkillsNet.SyncPlayerPassiveSkillsPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> ClientPassiveSkills.applyLevelsJson(payload.json()));
    }

    public static void handleUpgradeResult(PassiveSkillsNet.UpgradeResultPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (context.client().player != null && payload.message() != null && !payload.message().isBlank()) {
                context.client().player.sendMessage(Text.literal(payload.message()), false);
            }
        });
    }
}
