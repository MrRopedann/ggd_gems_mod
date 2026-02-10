package su.ggd.ggd_gems_mod.socket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SocketedGem(String type, int level) {
    public static final Codec<SocketedGem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("type").forGetter(SocketedGem::type),
            Codec.INT.fieldOf("level").forGetter(SocketedGem::level)
    ).apply(inst, SocketedGem::new));
}
