package su.ggd.ggd_gems_mod.currency.server;

import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.currency.CurrencyService;

/**
 * ВНУТРЕННИЙ доступ к серверному балансу валюты.
 *
 * Single source of truth: {@link CurrencyService} (PersistentState + sync).
 *
 * ВАЖНО: вызывать только на серверном треде.
 */
public final class CurrencyServerAccess {
    private CurrencyServerAccess() {}

    public static long get(ServerPlayerEntity player) {
        return CurrencyService.getBalance(player);
    }

    public static void set(ServerPlayerEntity player, long value) {
        CurrencyService.setBalance(player, Math.max(0L, value));
    }
}