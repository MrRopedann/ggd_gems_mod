package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public interface OnBlockBreakAfter {
    void onBlockBreakAfter(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    );
}
