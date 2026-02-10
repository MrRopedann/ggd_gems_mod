package su.ggd.ggd_gems_mod.inventory.sort.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class InventorySortNet {
    private InventorySortNet() {}

    public static final CustomPayload.Id<SortRequestPayload> SORT_REQUEST_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "sort_request"));

    public static final PacketCodec<RegistryByteBuf, SortRequestPayload> SORT_REQUEST_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SortRequestPayload::sortType,
                    SortRequestPayload::new
            );

    public record SortRequestPayload(String sortType) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return SORT_REQUEST_ID;
        }
    }
}
