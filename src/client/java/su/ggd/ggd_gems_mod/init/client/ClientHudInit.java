package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.hud.ExperienceBarTextHud;
import su.ggd.ggd_gems_mod.hud.MobHealthBarHud;
import su.ggd.ggd_gems_mod.hud.XpGainPopupHud;
import su.ggd.ggd_gems_mod.quests.client.hud.QuestTrackerHud;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientHudInit {
    private ClientHudInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientHudInit")) return;

        MobHealthBarHud.init();
        ExperienceBarTextHud.init();
        XpGainPopupHud.init();
        QuestTrackerHud.init();
    }
}
