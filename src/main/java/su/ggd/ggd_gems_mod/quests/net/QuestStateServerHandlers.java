package su.ggd.ggd_gems_mod.quests.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.quests.service.QuestService;

public final class QuestStateServerHandlers {
    private QuestStateServerHandlers() {}

    public static void handleStart(QuestStateNet.C2SStartQuestPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity player = ctx.player();
        if (player == null) return;

        ctx.server().execute(() -> QuestService.startQuest(player, payload.questId()));
    }

    public static void handleAbandon(QuestStateNet.C2SAbandonQuestPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity player = ctx.player();
        if (player == null) return;

        ctx.server().execute(() -> QuestService.abandonQuest(player, payload.questId()));
    }

    public static void handleTrack(QuestStateNet.C2STrackQuestPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity player = ctx.player();
        if (player == null) return;

        ctx.server().execute(() -> QuestService.trackQuest(player, payload.questIdOrNone()));
    }
}
