package su.ggd.ggd_gems_mod.quests.client.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import su.ggd.ggd_gems_mod.quests.net.QuestStateNet;

public final class QuestClientNet {
    private QuestClientNet() {}

    public static void startQuest(String questId) {
        if (questId == null || questId.isBlank()) return;
        ClientPlayNetworking.send(new QuestStateNet.C2SStartQuestPayload(questId));
    }

    public static void abandonQuest(String questId) {
        if (questId == null || questId.isBlank()) return;
        ClientPlayNetworking.send(new QuestStateNet.C2SAbandonQuestPayload(questId));
    }

    public static void trackQuest(String questIdOrNone) {
        if (questIdOrNone == null || questIdOrNone.isBlank()) questIdOrNone = "none";
        ClientPlayNetworking.send(new QuestStateNet.C2STrackQuestPayload(questIdOrNone));
    }
}
