package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public interface ModifyIncomingDamage {
    /**
     * @return новое значение входящего урона (можно вернуть исходное amount без изменений)
     */
    float modifyIncomingDamage(
            ServerWorld world,
            ServerPlayerEntity player,
            DamageSource source,
            float amount,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    );
}
