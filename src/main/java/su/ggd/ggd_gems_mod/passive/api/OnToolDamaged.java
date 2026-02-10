package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

/**
 * Хук вызывается когда мы зафиксировали, что инструмент реально потратил прочность.
 * Реализуется без mixin: событие фиксируется в hooks (block break / use-on-block).
 */
public interface OnToolDamaged {
    void onToolDamaged(
            ServerWorld world,
            ServerPlayerEntity player,
            ItemStack tool,
            int damageTaken,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    );
}
