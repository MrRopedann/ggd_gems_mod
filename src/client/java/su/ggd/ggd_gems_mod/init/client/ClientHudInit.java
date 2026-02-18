package su.ggd.ggd_gems_mod.init.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import su.ggd.ggd_gems_mod.hud.ExperienceBarTextHud;
import su.ggd.ggd_gems_mod.hud.MobHealthBarHud;
import su.ggd.ggd_gems_mod.hud.XpGainPopupHud;
import su.ggd.ggd_gems_mod.quests.client.hud.QuestTrackerHud;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientHudInit {
    private ClientHudInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientHudInit")) return;

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MobHealthBarHud.render(ctx, tickCounter);
            ExperienceBarTextHud.render(ctx, tickCounter);
            XpGainPopupHud.render(ctx, tickCounter);
            QuestTrackerHud.render(ctx, tickCounter);
        });
    }
}
