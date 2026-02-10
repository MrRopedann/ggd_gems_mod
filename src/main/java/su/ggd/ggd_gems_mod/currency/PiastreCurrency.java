package su.ggd.ggd_gems_mod.currency;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import su.ggd.ggd_gems_mod.registry.ModItems;

public final class PiastreCurrency {
    private PiastreCurrency() {}

    public static boolean has(PlayerEntity player, int amount) {
        if (amount <= 0) return true;
        if (player == null) return false;
        if (player.isCreative()) return true;

        int found = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(ModItems.PIASTRE)) {
                found += s.getCount();
                if (found >= amount) return true;
            }
        }
        return false;
    }

    public static void consume(PlayerEntity player, int amount) {
        if (amount <= 0) return;
        if (player == null) return;
        if (player.isCreative()) return;

        int left = amount;
        var inv = player.getInventory();

        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isOf(ModItems.PIASTRE)) continue;

            int take = Math.min(left, s.getCount());
            s.decrement(take);
            left -= take;
        }
    }
}
