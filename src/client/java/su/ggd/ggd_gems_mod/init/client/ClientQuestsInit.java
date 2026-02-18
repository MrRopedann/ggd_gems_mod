package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.quests.client.ui.QuestJournalClient;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientQuestsInit {
    private ClientQuestsInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientQuestsInit")) return;
        QuestJournalClient.init();
    }
}
