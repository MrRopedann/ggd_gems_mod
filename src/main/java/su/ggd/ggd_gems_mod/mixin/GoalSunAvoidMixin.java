package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.entity.ai.goal.AvoidSunlightGoal;
import net.minecraft.entity.ai.goal.EscapeSunlightGoal;
import net.minecraft.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.ggd.ggd_gems_mod.config.MobRulesConfigManager;

@Mixin(Goal.class)
public abstract class GoalSunAvoidMixin {

    @Inject(method = "shouldContinue", at = @At("HEAD"), cancellable = true, require = 0)
    private void ggd$stopSunAvoidContinue(CallbackInfoReturnable<Boolean> cir) {
        if (!MobRulesConfigManager.get().disableDaylightBurn) return;

        Object self = this;
        if (self instanceof AvoidSunlightGoal || self instanceof EscapeSunlightGoal) {
            cir.setReturnValue(false);
        }
    }
}
