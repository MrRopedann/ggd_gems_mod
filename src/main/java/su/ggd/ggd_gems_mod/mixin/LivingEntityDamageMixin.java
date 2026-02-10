package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.state.PassiveSkillsState;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    /**
     * Модифицируем amount до того, как игра применит урон.
     * Сигнатура LivingEntity#damage(ServerWorld, DamageSource, float) в 1.21.x.
     */
    @ModifyVariable(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float ggd$passive_modifyRangedDamage(float amount, ServerWorld world, DamageSource source) {
        if (amount <= 0f) return amount;

        // атакующий
        if (!(source.getAttacker() instanceof ServerPlayerEntity attackerPlayer)) return amount;

        var cfg = PassiveSkillsConfigManager.get();
        if (cfg == null || cfg.byId == null) return amount;

        // цель
        LivingEntity target = (LivingEntity) (Object) this;

        float out = amount;

        if (!Float.isFinite(out)) return amount;
        return MathHelper.clamp(out, 0.0f, 2048.0f);
    }

}
