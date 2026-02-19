package su.ggd.ggd_gems_mod.currency.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.currency.CurrencyService;
import su.ggd.ggd_gems_mod.currency.net.BankNet;

public final class BankServerHandlers {
    private BankServerHandlers() {}

    private static Item getPiastreItem() {
        Item it = Registries.ITEM.get(Identifier.of("ggd_gems_mod", "piastre"));
        return (it == null || it == Items.AIR) ? null : it;
    }

    public static void handleWithdraw(BankNet.WithdrawPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity p = ctx.player();
        int amount = payload == null ? 0 : payload.amount();
        if (amount <= 0) return;

        ctx.server().execute(() -> {
            long bal = CurrencyService.getBalance(p);
            if (bal < amount) return;

            Item piastre = getPiastreItem();
            if (piastre == null) return;

            // списываем баланс
            if (!CurrencyService.trySpend(p, amount)) return;

            // выдаём предметами (с разбиением по 64)
            int left = amount;
            while (left > 0) {
                int give = Math.min(64, left);
                ItemStack st = new ItemStack(piastre, give);
                boolean ok = p.getInventory().insertStack(st);
                if (!ok || !st.isEmpty()) p.dropItem(st, false);
                left -= give;
            }
        });
    }

    public static void handleDepositAll(BankNet.DepositAllPayload payload, ServerPlayNetworking.Context ctx) {
        ServerPlayerEntity p = ctx.player();

        ctx.server().execute(() -> {
            Item piastre = getPiastreItem();
            if (piastre == null) return;

            int found = 0;
            var inv = p.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (st.isEmpty() || st.getItem() != piastre) continue;
                int c = st.getCount();
                if (c <= 0) continue;
                found += c;
                inv.setStack(i, ItemStack.EMPTY);
            }

            if (found > 0) CurrencyService.add(p, found);
        });
    }
}
