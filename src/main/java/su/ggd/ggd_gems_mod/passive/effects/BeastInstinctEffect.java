package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.api.OnEntityDamagedByPlayer;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public final class BeastInstinctEffect implements PassiveSkillEffect, OnEntityDamagedByPlayer {

    @Override
    public String effectId() {
        return "beast_instinct";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        // фиксированный скилл: 1 уровень, 50 стоимости
        boolean changed = false;
        if (def.maxLevel != 1) { def.maxLevel = 1; changed = true; }
        if (def.costBase != 50) { def.costBase = 50; changed = true; }
        if (def.costPerLevel != 0) { def.costPerLevel = 0; changed = true; }
        return changed;
    }

    @Override
    public void onEntityDamagedByPlayer(ServerWorld world, ServerPlayerEntity player, LivingEntity victim,
                                        DamageSource source, float amount,
                                        PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (level <= 0) return;
        if (!(victim instanceof PassiveEntity)) return;

        // не трогаем жителей/торговцев
        if (victim instanceof VillagerEntity) return;
        if (victim instanceof WanderingTraderEntity) return;

        // Смысл: после удара не "убегают".
        // Без mixin надёжнее всего зафиксировать движение кратко невидимым замедлением + стоп навигации.
        victim.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                40,      // 2 секунды
                10,      // сильно замедляет (обычно фактически стоп)
                true,    // ambient
                false,   // showParticles
                false    // showIcon
        ));

        if (victim instanceof MobEntity mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }
}
