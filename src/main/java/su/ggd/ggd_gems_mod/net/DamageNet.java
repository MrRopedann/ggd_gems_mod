package su.ggd.ggd_gems_mod.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class DamageNet {
    private DamageNet() {}

    public static final CustomPayload.Id<DamageIndicatorPayload> DAMAGE_INDICATOR_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "damage_indicator"));

    public static final PacketCodec<RegistryByteBuf, DamageIndicatorPayload> DAMAGE_INDICATOR_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, DamageIndicatorPayload::entityId,
                    PacketCodecs.FLOAT, DamageIndicatorPayload::amount,
                    DamageIndicatorPayload::new
            );

    public record DamageIndicatorPayload(int entityId, float amount) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return DAMAGE_INDICATOR_ID;
        }
    }
}
