package su.ggd.ggd_gems_mod.currency;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.currency.net.CurrencyNet;

public final class CurrencySyncServer {
    private CurrencySyncServer() {}

    public static void sendTo(ServerPlayerEntity player, long balance) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new CurrencyNet.SyncBalancePayload(Math.max(0L, balance)));
    }

    public static void sendCurrentTo(ServerPlayerEntity player) {
        if (player == null) return;
        sendTo(player, CurrencyService.getBalance(player));
    }
}
