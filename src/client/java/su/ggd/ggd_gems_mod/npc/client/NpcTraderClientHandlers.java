package su.ggd.ggd_gems_mod.npc.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import su.ggd.ggd_gems_mod.client.menu.trader.TraderScreen;
import su.ggd.ggd_gems_mod.npc.net.NpcTraderNet;

public final class NpcTraderClientHandlers {
    private NpcTraderClientHandlers() {}

    public static void handleOpen(NpcTraderNet.OpenTraderPayload payload, ClientPlayNetworking.Context ctx) {
        MinecraftClient client = ctx.client();
        if (client == null) return;

        client.execute(() -> {
            TraderClientState.apply(payload.traderId(), payload.traderName(), payload.trades());
            client.setScreen(new TraderScreen());
        });
    }
}
