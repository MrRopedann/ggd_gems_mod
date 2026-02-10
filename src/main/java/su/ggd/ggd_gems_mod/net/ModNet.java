package su.ggd.ggd_gems_mod.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class ModNet {
    private ModNet() {}

    public static final CustomPayload.Id<SyncGemsConfigPayload> SYNC_GEMS_CONFIG_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "sync_gems_config"));

    public static final PacketCodec<RegistryByteBuf, SyncGemsConfigPayload> SYNC_GEMS_CONFIG_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SyncGemsConfigPayload::hash,
                    PacketCodecs.STRING, SyncGemsConfigPayload::json,
                    SyncGemsConfigPayload::new
            );

    public record SyncGemsConfigPayload(String hash, String json) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return SYNC_GEMS_CONFIG_ID;
        }
    }
}
