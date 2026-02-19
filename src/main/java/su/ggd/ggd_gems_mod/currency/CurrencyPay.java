package su.ggd.ggd_gems_mod.currency;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CurrencyPay {
    private CurrencyPay() {}

    public static boolean has(PlayerEntity player, int amount) {
        if (amount <= 0) return true;
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        return CurrencyService.getBalance(sp) >= amount;
    }

    public static boolean take(PlayerEntity player, int amount) {
        if (amount <= 0) return true;
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        return CurrencyService.trySpend(sp, amount);
    }
}
