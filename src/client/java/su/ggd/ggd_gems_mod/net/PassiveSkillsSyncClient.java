package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.passive.ClientPassiveSkills;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;

public final class PassiveSkillsSyncClient {
    private PassiveSkillsSyncClient() {}

    private static String lastHash;

    public static void init() {
        // S2C: конфиг
        ClientPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.SYNC_PASSIVES_CONFIG_ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.hash() != null && payload.hash().equals(lastHash)) return;
                lastHash = payload.hash();

                PassiveSkillsConfigManager.applyFromNetworkJson(payload.json());

                if (context.client().player != null) {
                    context.client().player.sendMessage(Text.literal("§a[Passives] Config synced from server."), true);
                }
            });
        });

        // S2C: уровни игрока
        ClientPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.SYNC_PLAYER_PASSIVES_ID, (payload, context) -> {
            context.client().execute(() -> ClientPassiveSkills.applyLevelsJson(payload.json()));
        });

        // S2C: результат улучшения (если используешь)
        ClientPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.UPGRADE_RESULT_ID, (payload, context) -> {
            context.client().execute(() -> {
                // Тут вызывай UI-уведомление в экране/чат
                if (context.client().player != null && payload.message() != null && !payload.message().isBlank()) {
                    context.client().player.sendMessage(Text.literal(payload.message()), false);
                }
            });
        });
    }
}
