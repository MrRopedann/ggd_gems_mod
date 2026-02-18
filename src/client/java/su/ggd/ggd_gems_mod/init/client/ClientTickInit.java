package su.ggd.ggd_gems_mod.init.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import su.ggd.ggd_gems_mod.combat.DamagePopupTracker;
import su.ggd.ggd_gems_mod.hud.XpGainPopupHud;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientTickInit {
    private ClientTickInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientTickInit")) return;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // hud tick
            XpGainPopupHud.tick(client);

            // combat trackers
            DamagePopupTracker.tick(client);

            // keybinds handling
            ClientKeybindsInit.tick(client);
        });
    }
}
