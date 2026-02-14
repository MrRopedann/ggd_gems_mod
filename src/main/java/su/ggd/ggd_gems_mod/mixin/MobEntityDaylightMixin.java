package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.ggd.ggd_gems_mod.config.MobRulesConfigManager;

@Mixin(MobEntity.class)
public abstract class MobEntityDaylightMixin {

    @Inject(method = "isAffectedByDaylight", at = @At("HEAD"), cancellable = true, require = 0)
    private void ggd$disableDaylightBurn(CallbackInfoReturnable<Boolean> cir) {
        if (MobRulesConfigManager.get().disableDaylightBurn) {
            cir.setReturnValue(false);
        }
    }
}
