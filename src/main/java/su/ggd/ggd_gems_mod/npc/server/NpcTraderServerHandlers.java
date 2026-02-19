package su.ggd.ggd_gems_mod.npc.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.config.NpcTradersConfig;
import su.ggd.ggd_gems_mod.config.NpcTradersConfigManager;
import su.ggd.ggd_gems_mod.currency.CurrencyService;
import su.ggd.ggd_gems_mod.gem.GemRegistry;
import su.ggd.ggd_gems_mod.gem.GemStacks;
import su.ggd.ggd_gems_mod.npc.net.NpcTraderNet;
import su.ggd.ggd_gems_mod.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NpcTraderServerHandlers {
    private NpcTraderServerHandlers() {}

    public static void sendOpen(ServerPlayerEntity player, String traderIdRaw) {
        if (player == null) return;

        String traderId = normId(traderIdRaw);
        NpcTradersConfig cfg = NpcTradersConfigManager.get();
        if (cfg == null) return;

        NpcTradersConfig.TraderDef def = cfg.byId.get(traderId);
        if (def == null) return;

        List<NpcTraderNet.TradeEntry> entries = buildEntries(def);
        ServerPlayNetworking.send(player, new NpcTraderNet.OpenTraderPayload(traderId, def.name, entries));
    }

    public static void handleBuy(NpcTraderNet.BuyPayload payload, ServerPlayNetworking.Context ctx) {
        if (payload == null || ctx == null) return;

        ServerPlayerEntity player = ctx.player();
        if (player == null) return;

        String traderId = normId(payload.traderId());
        int idx = payload.tradeIndex();
        if (idx < 0) return;

        NpcTradersConfig cfg = NpcTradersConfigManager.get();
        if (cfg == null) return;

        NpcTradersConfig.TraderDef def = cfg.byId.get(traderId);
        if (def == null || def.trades == null || idx >= def.trades.size()) return;

        NpcTradersConfig.TradeDef tr = def.trades.get(idx);
        if (tr == null || tr.buy == null) return;

        long price = Math.max(0L, tr.buy.count); // buy.count = виртуальная цена
        ItemStack out = buildSellStack(tr);
        if (out.isEmpty()) return;

        if (!CurrencyService.trySpend(player, price)) {
            return;
        }

        boolean ok = player.getInventory().insertStack(out);
        if (!ok) player.dropItem(out, false);
    }

    private static List<NpcTraderNet.TradeEntry> buildEntries(NpcTradersConfig.TraderDef def) {
        List<NpcTraderNet.TradeEntry> out = new ArrayList<>();
        if (def.trades == null) return out;

        for (NpcTradersConfig.TradeDef tr : def.trades) {
            if (tr == null || tr.buy == null) continue;

            long price = Math.max(0L, tr.buy.count);
            ItemStack sell = buildSellStack(tr);
            if (sell.isEmpty()) continue;

            // ВАЖНО: ItemStack в payload не должен быть "пустым" и лучше с реальным count.
            out.add(new NpcTraderNet.TradeEntry(price, sell));
        }
        return out;
    }

    private static ItemStack buildSellStack(NpcTradersConfig.TradeDef t) {
        if (t == null) return ItemStack.EMPTY;

        if (t.sellGem != null) {
            String type = normId(t.sellGem.type);
            if (type.isBlank()) return ItemStack.EMPTY;
            if (!GemRegistry.all().containsKey(type)) return ItemStack.EMPTY;

            ItemStack gem = new ItemStack(ModItems.GEM, 1);
            GemStacks.applyGemDataFromConfig(gem, type, Math.max(1, t.sellGem.level));
            return gem;
        }

        if (t.sell != null) {
            Identifier id = Identifier.tryParse(safe(t.sell.item));
            if (id == null) return ItemStack.EMPTY;

            Item it = Registries.ITEM.get(id);
            if (it == null || it == Items.AIR) return ItemStack.EMPTY;

            return new ItemStack(it, Math.max(1, t.sell.count));
        }

        return ItemStack.EMPTY;
    }

    private static String normId(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
