package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.combat.DamagePopupTracker;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientCombatInit {
    private ClientCombatInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientCombatInit")) return;
        DamagePopupTracker.init();
    }
}
