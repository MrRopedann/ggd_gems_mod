package su.ggd.ggd_gems_mod.currency.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class BankNet {
    private BankNet() {}

    public static final CustomPayload.Id<WithdrawPayload> WITHDRAW_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "bank_withdraw"));
    public static final PacketCodec<RegistryByteBuf, WithdrawPayload> WITHDRAW_CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, WithdrawPayload::amount, WithdrawPayload::new);

    public record WithdrawPayload(int amount) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return WITHDRAW_ID; }
    }

    public static final CustomPayload.Id<DepositAllPayload> DEPOSIT_ALL_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "bank_deposit_all"));
    public static final PacketCodec<RegistryByteBuf, DepositAllPayload> DEPOSIT_ALL_CODEC =
            PacketCodec.unit(new DepositAllPayload());

    public record DepositAllPayload() implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return DEPOSIT_ALL_ID; }
    }
}
