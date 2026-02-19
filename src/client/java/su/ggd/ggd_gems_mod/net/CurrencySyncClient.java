package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import su.ggd.ggd_gems_mod.currency.client.CurrencyClientState;
import su.ggd.ggd_gems_mod.currency.net.CurrencyNet;

public final class CurrencySyncClient {
    private CurrencySyncClient() {}

    public static void handle(CurrencyNet.SyncBalancePayload payload, ClientPlayNetworking.Context ctx) {
        if (payload == null) return;
        ctx.client().execute(() -> CurrencyClientState.set(payload.balance()));
    }
}
