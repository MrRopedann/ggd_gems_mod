package su.ggd.ggd_gems_mod.npc.net;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

import java.util.List;

public final class NpcTraderNet {
    private NpcTraderNet() {}

    public static final CustomPayload.Id<OpenTraderPayload> OPEN_TRADER_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "npc_trader_open"));

    public record TradeEntry(long price, ItemStack stack) {}

    public static final PacketCodec<RegistryByteBuf, TradeEntry> TRADE_ENTRY_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_LONG, TradeEntry::price,
                    ItemStack.PACKET_CODEC, TradeEntry::stack,
                    TradeEntry::new
            );

    public static final PacketCodec<RegistryByteBuf, OpenTraderPayload> OPEN_TRADER_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, OpenTraderPayload::traderId,
                    PacketCodecs.STRING, OpenTraderPayload::traderName,
                    PacketCodecs.collection(java.util.ArrayList::new, TRADE_ENTRY_CODEC), OpenTraderPayload::trades,
                    OpenTraderPayload::new
            );

    public record OpenTraderPayload(String traderId, String traderName, List<TradeEntry> trades) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return OPEN_TRADER_ID; }
    }

    public static final CustomPayload.Id<BuyPayload> BUY_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "npc_trader_buy"));

    public static final PacketCodec<RegistryByteBuf, BuyPayload> BUY_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, BuyPayload::traderId,
                    PacketCodecs.VAR_INT, BuyPayload::tradeIndex,
                    BuyPayload::new
            );

    public record BuyPayload(String traderId, int tradeIndex) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return BUY_ID; }
    }
}
