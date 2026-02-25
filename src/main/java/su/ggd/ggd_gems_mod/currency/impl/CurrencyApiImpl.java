package su.ggd.ggd_gems_mod.currency.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.currency.api.CurrencyApi;
import su.ggd.ggd_gems_mod.currency.api.CurrencyEvents;
import su.ggd.ggd_gems_mod.currency.server.CurrencyServerAccess;

/**
 * Реализация CurrencyApi через внутренний серверный доступ.
 * ВАЖНО: все операции должны вызываться только на серверном треде.
 */
public final class CurrencyApiImpl implements CurrencyApi {

    @Override
    public long get(ServerPlayerEntity player) {
        if (player == null) return 0L;
        return CurrencyServerAccess.get(player);
    }

    @Override
    public void set(ServerPlayerEntity player, long value) {
        if (player == null) return;
        long v = Math.max(0L, value);

        long old = CurrencyServerAccess.get(player);
        if (old == v) return;

        CurrencyServerAccess.set(player, v);
        CurrencyEvents.BALANCE_CHANGED.invoker().onBalanceChanged(player, old, v, "set");
    }

    @Override
    public void add(ServerPlayerEntity player, long amount) {
        if (player == null) return;
        if (amount <= 0) return;

        long old = CurrencyServerAccess.get(player);
        long next = safeAddClamp(old, amount);
        if (old == next) return;

        CurrencyServerAccess.set(player, next);
        CurrencyEvents.BALANCE_CHANGED.invoker().onBalanceChanged(player, old, next, "add");
    }

    @Override
    public boolean take(ServerPlayerEntity player, long amount) {
        if (player == null) return false;
        if (amount <= 0) return true;

        long old = CurrencyServerAccess.get(player);
        if (old < amount) return false;

        long next = old - amount;
        CurrencyServerAccess.set(player, next);
        CurrencyEvents.BALANCE_CHANGED.invoker().onBalanceChanged(player, old, next, "take");
        return true;
    }

    private static long safeAddClamp(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        long r = a + b;
        return Math.max(0L, r);
    }
}