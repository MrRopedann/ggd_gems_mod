package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientInitPlan {
    private ClientInitPlan() {}

    public static void init() {
        if (!InitOnce.markDone("ClientInitPlan")) return;

        ClientNetInit.init();
        ClientHudInit.init();
        ClientTooltipsInit.init();

        ClientCombatInit.init();
        ClientInventorySortInit.init();
        ClientPassiveInit.init();
        ClientQuestsInit.init();

        ClientKeybindsInit.init();
        ClientScreensInit.init();
    }
}
