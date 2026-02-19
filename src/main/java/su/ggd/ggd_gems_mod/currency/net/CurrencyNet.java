package su.ggd.ggd_gems_mod.currency.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class CurrencyNet {
    private CurrencyNet() {}

    public static final CustomPayload.Id<SyncBalancePayload> SYNC_BALANCE_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "sync_currency_balance"));

    public static final PacketCodec<RegistryByteBuf, SyncBalancePayload> SYNC_BALANCE_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_LONG, SyncBalancePayload::balance,
                    SyncBalancePayload::new
            );

    public record SyncBalancePayload(long balance) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return SYNC_BALANCE_ID; }
    }
}
