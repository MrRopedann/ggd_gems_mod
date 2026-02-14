package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.entity.ai.goal.AvoidSunlightGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.ggd.ggd_gems_mod.config.MobRulesConfigManager;

@Mixin(AvoidSunlightGoal.class)
public abstract class AvoidSunlightGoalMixin {

    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true, require = 0)
    private void ggd$disableAvoidSun(CallbackInfoReturnable<Boolean> cir) {
        if (MobRulesConfigManager.get().disableDaylightBurn) {
            cir.setReturnValue(false);
        }
    }
}
