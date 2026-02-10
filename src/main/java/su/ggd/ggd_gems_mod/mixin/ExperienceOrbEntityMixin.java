package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.ggd.ggd_gems_mod.passive.PassiveSkillDispatcher;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {

    @Shadow @Final protected static TrackedData<Integer> VALUE;
    @Shadow public abstract int getValue();
    @Shadow protected abstract void setValue(int value);

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void ggd$xpBonus(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity sp)) return;

        ExperienceOrbEntity orb = (ExperienceOrbEntity) (Object) this;
        if (orb.getEntityWorld().isClient()) return;
        if (!(orb.getEntityWorld() instanceof ServerWorld sw)) return;

        int base = this.getValue();
        if (base <= 0) return;

        int boosted = PassiveSkillDispatcher.applyXpOrb(sw, sp, base);
        boosted = MathHelper.clamp(boosted, 0, 32767);

        if (boosted != base) this.setValue(boosted);
    }
}
