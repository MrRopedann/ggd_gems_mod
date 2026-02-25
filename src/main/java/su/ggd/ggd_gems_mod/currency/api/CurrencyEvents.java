package su.ggd.ggd_gems_mod.currency.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CurrencyEvents {
    private CurrencyEvents() {}

    /**
     * Вызывается ПОСЛЕ изменения баланса.
     * oldValue -> newValue (newValue уже записан)
     */
    public static final Event<BalanceChanged> BALANCE_CHANGED =
            EventFactory.createArrayBacked(BalanceChanged.class, listeners -> (player, oldValue, newValue, reason) -> {
                for (BalanceChanged l : listeners) l.onBalanceChanged(player, oldValue, newValue, reason);
            });

    @FunctionalInterface
    public interface BalanceChanged {
        void onBalanceChanged(ServerPlayerEntity player, long oldValue, long newValue, String reason);
    }
}