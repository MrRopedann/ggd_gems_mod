package su.ggd.ggd_gems_mod.quests.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.quests.service.QuestService;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class QuestStateServerHandlers {
    private QuestStateServerHandlers() {}

    public static void init() {
        if (!InitOnce.markDone("QuestStateServerHandlers")) return;

        ServerPlayNetworking.registerGlobalReceiver(QuestStateNet.C2S_START_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> QuestService.startQuest(player, payload.questId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(QuestStateNet.C2S_ABANDON_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> QuestService.abandonQuest(player, payload.questId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(QuestStateNet.C2S_TRACK_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> QuestService.trackQuest(player, payload.questIdOrNone()));
        });
    }
}
