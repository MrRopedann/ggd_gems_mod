package su.ggd.ggd_gems_mod.currency;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class CurrencyPay {
    private CurrencyPay() {}

    public static boolean has(PlayerEntity player, Item currencyItem, int amount) {
        if (amount <= 0) return true;
        if (player == null || currencyItem == null) return false;

        int found = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && s.isOf(currencyItem)) {
                found += s.getCount();
                if (found >= amount) return true;
            }
        }
        return false;
    }

    public static boolean take(PlayerEntity player, Item currencyItem, int amount) {
        if (amount <= 0) return true;
        if (player == null || currencyItem == null) return false;

        int left = amount;
        var inv = player.getInventory();

        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !s.isOf(currencyItem)) continue;

            int can = Math.min(left, s.getCount());
            s.decrement(can);
            left -= can;

            if (left <= 0) return true;
        }
        return false;
    }
}
