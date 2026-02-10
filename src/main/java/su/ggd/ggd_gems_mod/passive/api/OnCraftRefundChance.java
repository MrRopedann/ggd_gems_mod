package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public interface OnCraftRefundChance {
    /**
     * @return шанс [0..1] на возврат 1 случайного ингредиента из рецепта
     */
    double getRefundChance(ServerWorld world, ServerPlayerEntity player, ItemStack crafted,
                           PassiveSkillsConfig.PassiveSkillDef def, int level);
}
