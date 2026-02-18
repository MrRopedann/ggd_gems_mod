package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.quests.client.QuestClientState;
import su.ggd.ggd_gems_mod.quests.net.QuestStateNet;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class QuestStateClientHandlers {
    private QuestStateClientHandlers() {}

    public static void init() {
        if (!InitOnce.markDone("QuestStateClientHandlers")) return;

        ClientPlayNetworking.registerGlobalReceiver(QuestStateNet.SYNC_PLAYER_STATE_ID, (payload, context) -> {
            context.client().execute(() -> {
                QuestClientState.applyPlayerJson(payload.json());

                if (context.client().player != null) {
                    context.client().player.sendMessage(Text.literal("§a[Quests] Player state synced."), true);
                }
            });
        });
    }
}
