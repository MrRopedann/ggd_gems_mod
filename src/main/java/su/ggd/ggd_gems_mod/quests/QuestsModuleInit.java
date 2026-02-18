package su.ggd.ggd_gems_mod.quests;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class QuestsModuleInit {
    private QuestsModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("QuestsModuleInit")) return;

        QuestHooks.init();
        TalkHook.init();
    }
}
