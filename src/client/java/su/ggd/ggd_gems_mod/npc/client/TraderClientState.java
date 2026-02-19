package su.ggd.ggd_gems_mod.npc.client;

import net.minecraft.item.ItemStack;
import su.ggd.ggd_gems_mod.npc.net.NpcTraderNet;

import java.util.Collections;
import java.util.List;

public final class TraderClientState {
    private TraderClientState() {}

    public record TradeView(long price, ItemStack stack) {}

    private static String traderId;
    private static String traderName;
    private static List<TradeView> trades = Collections.emptyList();

    public static String traderId() { return traderId; }
    public static String traderName() { return traderName; }
    public static List<TradeView> trades() { return trades; }

    public static void apply(String id, String name, List<NpcTraderNet.TradeEntry> entries) {
        traderId = id;
        traderName = name;

        if (entries == null || entries.isEmpty()) {
            trades = Collections.emptyList();
            return;
        }

        trades = entries.stream()
                .filter(e -> e != null && e.stack() != null && !e.stack().isEmpty())
                .map(e -> new TradeView(Math.max(0L, e.price()), e.stack()))
                .toList();
    }
}
