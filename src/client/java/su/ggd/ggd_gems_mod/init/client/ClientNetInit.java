package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.net.DamageIndicatorClient;
import su.ggd.ggd_gems_mod.net.GemsConfigSyncClient;
import su.ggd.ggd_gems_mod.net.MobLevelSyncClient;
import su.ggd.ggd_gems_mod.net.PassiveSkillsSyncClient;
import su.ggd.ggd_gems_mod.net.QuestStateClientHandlers;
import su.ggd.ggd_gems_mod.net.QuestsSyncClient;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientNetInit {
    private ClientNetInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientNetInit")) return;

        GemsConfigSyncClient.init();
        PassiveSkillsSyncClient.init();

        QuestsSyncClient.init();
        QuestStateClientHandlers.init();

        MobLevelSyncClient.init();
        DamageIndicatorClient.init();
    }
}
