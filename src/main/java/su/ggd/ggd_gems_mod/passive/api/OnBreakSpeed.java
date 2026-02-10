package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public interface OnBreakSpeed {
    float modifyBreakSpeed(
            PlayerEntity player,
            BlockState state,
            ItemStack tool,
            float currentSpeed,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    );
}
