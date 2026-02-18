package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientScreensInit {
    private ClientScreensInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientScreensInit")) return;
        // currently empty
    }
}
