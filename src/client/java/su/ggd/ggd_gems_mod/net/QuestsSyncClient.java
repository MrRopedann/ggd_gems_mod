package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.quests.net.QuestNet;

public final class QuestsSyncClient {
    private QuestsSyncClient() {}

    private static String lastHash;

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(QuestNet.SYNC_QUEST_DEFS_ID, (payload, context) -> {
            context.client().execute(() -> {
                if (payload.hash() != null && payload.hash().equals(lastHash)) return;
                lastHash = payload.hash();

                QuestsConfigManager.applyFromNetworkJson(payload.json());

                if (context.client().player != null) {
                    context.client().player.sendMessage(Text.literal("§a[Quests] Definitions synced from server."), true);
                }
            });
        });
    }
}
