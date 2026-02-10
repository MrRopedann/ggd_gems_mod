package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.ggd.ggd_gems_mod.passive.PassiveSkillDispatcher;

@Mixin(PlayerEntity.class)
public class PlayerEntityBreakSpeedMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void ggd$passives_breakSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (state == null) return;

        ItemStack tool = player.getMainHandStack();
        if (tool == null || tool.isEmpty()) return;

        float base = cir.getReturnValue();
        float modified = PassiveSkillDispatcher.applyBreakSpeed(player, state, tool, base);

        if (modified != base) {
            cir.setReturnValue(modified);
        }
    }
}
