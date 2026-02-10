package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.ggd.ggd_gems_mod.passive.PassiveSkillDispatcher;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Mixin(CraftingResultSlot.class)
public class CraftingResultSlotMixin {

    @Unique
    private static Field ggd$inputInvField;

    @Unique
    private ItemStack[] ggd$before;

    @Unique
    private static Inventory ggd$getInputInv(Object self) {
        try {
            Field f = ggd$inputInvField;
            if (f == null) {
                // Ищем поле типа Inventory, которое похоже на крафтовую сетку
                for (Field ff : self.getClass().getDeclaredFields()) {
                    if (!Inventory.class.isAssignableFrom(ff.getType())) continue;
                    ff.setAccessible(true);

                    Object v = ff.get(self);
                    if (!(v instanceof Inventory inv)) continue;

                    int size = inv.size();
                    // крафтовая сетка: обычно 4 (2x2) или 9 (3x3)
                    if (size == 4 || size == 9 || (size > 0 && size <= 10)) {
                        ggd$inputInvField = ff;
                        f = ff;
                        break;
                    }
                }
            }
            if (f == null) return null;
            Object v = f.get(self);
            return (v instanceof Inventory inv) ? inv : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Inject(
            method = "onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private void ggd$craft_refund_capture(PlayerEntity player, ItemStack crafted, CallbackInfo ci) {
        Inventory input = ggd$getInputInv(this);
        if (input == null) return;

        ggd$before = new ItemStack[input.size()];
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getStack(i);
            ggd$before[i] = (s == null) ? ItemStack.EMPTY : s.copy();
        }
    }

    @Inject(
            method = "onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("TAIL")
    )
    private void ggd$craft_refund_apply(PlayerEntity player, ItemStack crafted, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity sp)) return;
        if (!(sp.getEntityWorld() instanceof ServerWorld sw)) return;
        if (crafted == null || crafted.isEmpty()) return;

        Inventory input = ggd$getInputInv(this);
        if (input == null) return;

        double chance = PassiveSkillDispatcher.getCraftRefundChance(sw, sp, crafted);
        if (chance <= 0.0) return;
        if (sp.getRandom().nextDouble() >= chance) return;

        ItemStack[] before = ggd$before;
        ggd$before = null;
        if (before == null || before.length != input.size()) return;

        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack b = before[i];
            ItemStack a = input.getStack(i);

            if (b == null || b.isEmpty()) continue;

            int bc = b.getCount();
            int ac = (a == null || a.isEmpty()) ? 0 : a.getCount();

            // слот уменьшился => ингредиент реально потрачен
            if (ac < bc) candidates.add(i);
        }

        if (candidates.isEmpty()) return;

        int idx = candidates.get(sp.getRandom().nextInt(candidates.size()));
        ItemStack b = before[idx];
        if (b == null || b.isEmpty()) return;

        ItemStack refund = b.copy();
        refund.setCount(1);

        boolean inserted = sp.getInventory().insertStack(refund);
        if (!inserted) sp.dropItem(refund, false);
    }
}
