package su.ggd.ggd_gems_mod.passive.util;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;

public final class OreTagUtil {
    private OreTagUtil() {}

    public static boolean isVanillaOre(BlockState state) {
        return state != null && (
                state.isIn(BlockTags.COAL_ORES) ||
                        state.isIn(BlockTags.IRON_ORES) ||
                        state.isIn(BlockTags.GOLD_ORES) ||
                        state.isIn(BlockTags.COPPER_ORES) ||
                        state.isIn(BlockTags.DIAMOND_ORES) ||
                        state.isIn(BlockTags.EMERALD_ORES) ||
                        state.isIn(BlockTags.LAPIS_ORES) ||
                        state.isIn(BlockTags.REDSTONE_ORES)
        );
    }
}
