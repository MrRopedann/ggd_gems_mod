package su.ggd.ggd_gems_mod.init.client;

import su.ggd.ggd_gems_mod.SocketedGemsTooltip;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientTooltipsInit {
    private ClientTooltipsInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientTooltipsInit")) return;
        SocketedGemsTooltip.init();
    }
}
