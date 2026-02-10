package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public interface OnEntityDamagedByPlayer {
    void onEntityDamagedByPlayer(
            ServerWorld world,
            ServerPlayerEntity player,
            LivingEntity victim,
            DamageSource source,
            float amount,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    );
}
