package su.ggd.ggd_gems_mod.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class MobLevelNet {
    private MobLevelNet() {}

    public static final CustomPayload.Id<MobLevelPayload> MOB_LEVEL_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "mob_level"));

    public static final PacketCodec<RegistryByteBuf, MobLevelPayload> MOB_LEVEL_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, MobLevelPayload::entityId,
                    PacketCodecs.VAR_INT, MobLevelPayload::level,
                    MobLevelPayload::new
            );

    public record MobLevelPayload(int entityId, int level) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return MOB_LEVEL_ID;
        }
    }
}
