package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.quests.client.QuestClientState;
import su.ggd.ggd_gems_mod.quests.net.QuestStateNet;

public final class QuestStateClientHandlers {
    private QuestStateClientHandlers() {}

    public static void handleSyncPlayerState(QuestStateNet.SyncPlayerQuestStatePayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            QuestClientState.applyPlayerJson(payload.json());

            if (context.client().player != null) {
                context.client().player.sendMessage(Text.literal("§a[Quests] Player state synced."), true);
            }
        });
    }
}
