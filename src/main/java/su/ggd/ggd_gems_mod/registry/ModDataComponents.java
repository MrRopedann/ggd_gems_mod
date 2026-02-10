package su.ggd.ggd_gems_mod.registry;

import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.mojang.serialization.Codec;
import java.util.List;

import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;
import su.ggd.ggd_gems_mod.socket.SocketedGem;


public final class ModDataComponents {
    private ModDataComponents() {}

    public static ComponentType<String> GEM_TYPE;
    public static ComponentType<Integer> GEM_LEVEL;
    public static ComponentType<List<SocketedGem>> SOCKETED_GEMS;

    public static void initialize() {
        if (GEM_TYPE != null) return; // защита от повторного вызова

        GEM_TYPE = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Ggd_gems_mod.MOD_ID, "gem_type"),
                ComponentType.<String>builder().codec(net.minecraft.util.dynamic.Codecs.NON_EMPTY_STRING).build()
        );

        GEM_LEVEL = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Ggd_gems_mod.MOD_ID, "gem_level"),
                ComponentType.<Integer>builder().codec(com.mojang.serialization.Codec.INT).build()
        );

        SOCKETED_GEMS = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Ggd_gems_mod.MOD_ID, "socketed_gems"),
                ComponentType.<List<SocketedGem>>builder()
                        .codec(Codec.list(SocketedGem.CODEC))
                        .build()
        );
    }
}
