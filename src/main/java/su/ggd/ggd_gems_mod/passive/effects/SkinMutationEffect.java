package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.api.OnPlayerDamaged;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public final class SkinMutationEffect implements PassiveSkillEffect, OnPlayerDamaged {

    private static final float DAMAGE_PER_LEVEL = 0.15f;

    @Override
    public String effectId() {
        return "skin_mutation";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        return false;
    }

    @Override
    public void onPlayerDamaged(ServerWorld world, ServerPlayerEntity player, DamageSource source, float amount,
                                PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (level <= 0) return;

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof MobEntity)) return;          // по ТЗ: только моб
        if (!(attacker instanceof LivingEntity living)) return;
        if (!living.isAlive()) return;

        float reflect = DAMAGE_PER_LEVEL * level;              // 10 lvl => 1.5
        if (reflect <= 0f) return;

        // В 1.21.x DamageSource берём из DamageSources мира
        DamageSource thornsLike = world.getDamageSources().thorns(player);

        // В 1.21.x нужен ServerWorld в параметрах damage()
        living.damage(world, thornsLike, reflect);
    }
}
