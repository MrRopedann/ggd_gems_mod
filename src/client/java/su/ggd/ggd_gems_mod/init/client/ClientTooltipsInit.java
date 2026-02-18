package su.ggd.ggd_gems_mod.init.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import su.ggd.ggd_gems_mod.SocketedGemsTooltip;
import su.ggd.ggd_gems_mod.gem.client.GemItemTooltip;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientTooltipsInit {
    private ClientTooltipsInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientTooltipsInit")) return;

        ItemTooltipCallback.EVENT.register((stack, ctx, type, tooltip) -> {
            GemItemTooltip.append(stack, tooltip);       // tooltip для самоцвета
            SocketedGemsTooltip.append(stack, tooltip);  // tooltip для предметов с сокетами
        });
    }
}
