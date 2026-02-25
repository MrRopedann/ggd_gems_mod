package su.ggd.ggd_gems_mod.currency.api;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Публичный API виртуальной валюты ggd_gems_mod.
 *
 * Истина на сервере. Все операции только серверные.
 */
public interface CurrencyApi {

    long get(ServerPlayerEntity player);

    void set(ServerPlayerEntity player, long value);

    /**
     * Пополнить баланс.
     * @param amount >= 0
     */
    void add(ServerPlayerEntity player, long amount);

    /**
     * Списать баланс.
     * @return true если успешно списано (хватило средств)
     * @param amount >= 0
     */
    boolean take(ServerPlayerEntity player, long amount);

    default boolean canAfford(ServerPlayerEntity player, long amount) {
        if (amount <= 0) return true;
        return get(player) >= amount;
    }
}