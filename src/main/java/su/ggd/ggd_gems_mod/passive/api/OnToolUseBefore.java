package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

/**
 * Вызывается ПЕРЕД действием, которое может потратить прочность инструмента.
 * Нужен, чтобы предотвратить поломку на "последней прочности" без mixin.
 */
public interface OnToolUseBefore {
    void onToolUseBefore(
            ServerWorld world,
            ServerPlayerEntity player,
            ItemStack tool,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    );
}
