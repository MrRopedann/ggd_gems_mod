package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.passive.BowTrajectoryRenderer;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientPassiveInit {
    private ClientPassiveInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientPassiveInit")) return;
        BowTrajectoryRenderer.init();
    }
}
