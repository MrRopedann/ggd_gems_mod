package su.ggd.ggd_gems_mod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.ggd.ggd_gems_mod.config.EconomyConfig;
import su.ggd.ggd_gems_mod.config.EconomyConfigManager;
import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.currency.CurrencyItems;
import su.ggd.ggd_gems_mod.currency.CurrencyPay;
import su.ggd.ggd_gems_mod.gem.GemAttributeApplier;
import su.ggd.ggd_gems_mod.gem.GemData;
import su.ggd.ggd_gems_mod.gem.GemStacks;
import su.ggd.ggd_gems_mod.registry.ModDataComponents;
import su.ggd.ggd_gems_mod.registry.ModItems;
import su.ggd.ggd_gems_mod.socket.SocketedGem;
import su.ggd.ggd_gems_mod.targeting.GemTargeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    private static final int OP_NONE = 0;
    private static final int OP_INSERT = 1;
    private static final int OP_COMBINE = 2;

    private static final int SLOT_LEFT = 0;
    private static final int SLOT_RIGHT = 1;
    private static final int SLOT_RESULT = 2;

    @Unique
    private long ggd$lastNoMoneyMsgTick = -9999;

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void ggd$updateResult(CallbackInfo ci) {
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;
        Property levelCostProp = ((AnvilScreenHandlerAccessor) (Object) this).ggd$getLevelCost();
        if (levelCostProp == null) return;

        ItemStack left = self.getSlot(SLOT_LEFT).getStack();
        ItemStack right = self.getSlot(SLOT_RIGHT).getStack();

        self.getSlot(SLOT_RESULT).setStack(ItemStack.EMPTY);
        levelCostProp.set(0);

        if (left == null || left.isEmpty()) return;
        if (right == null || right.isEmpty()) return;

        int op = detectOperation(left, right);
        if (op == OP_NONE) return;

        // gems config (types/targets/maxLevel)
        GemsConfig gemsCfg = GemsConfigManager.get();
        if (gemsCfg == null) return;
        if (gemsCfg.byType == null || gemsCfg.byType.isEmpty()) gemsCfg.rebuildIndex();

        // economy config (anvil costs)
        EconomyConfig eco = EconomyConfigManager.get();
        if (eco == null || eco.anvil == null) return;

        // ===== INSERT: предмет + самоцвет =====
        if (op == OP_INSERT) {
            String type = GemData.getType(right);
            int lvl = Math.max(1, GemData.getLevel(right));
            if (type == null || type.isBlank()) return;

            String key = type.trim().toLowerCase(Locale.ROOT);
            GemsConfig.GemDef def = gemsCfg.byType.get(key);
            if (def == null) return;

            // проверка targets
            if (!GemTargeting.canInsertGemInto(left, def)) return;

            ItemStack out = left.copy();

            List<SocketedGem> gems = out.get(ModDataComponents.SOCKETED_GEMS);
            if (gems == null) gems = new ArrayList<>();
            else gems = new ArrayList<>(gems);

            // 1) если уже есть самоцвет этого типа — заменяем уровень (только если новый выше)
            int idxSameType = -1;
            for (int i = 0; i < gems.size(); i++) {
                SocketedGem g = gems.get(i);
                if (g == null) continue;
                if (g.type() != null && g.type().equalsIgnoreCase(key)) {
                    idxSameType = i;
                    break;
                }
            }

            if (idxSameType >= 0) {
                SocketedGem existing = gems.get(idxSameType);
                int existingLvl = existing == null ? 0 : existing.level();

                // если новый уровень НЕ выше — запрещаем (не даём результат)
                if (lvl <= existingLvl) return;

                // заменяем
                gems.set(idxSameType, new SocketedGem(key, lvl));
            } else {
                // 2) иначе — добавляем в свободный слот (макс 2)
                if (gems.size() >= 2) return;
                gems.add(new SocketedGem(key, lvl));
            }

            out.set(ModDataComponents.SOCKETED_GEMS, gems);
            GemAttributeApplier.rebuild(out);
            self.getSlot(SLOT_RESULT).setStack(out);

            int cost = computeCost(eco.anvil.insertBase, eco.anvil.insertPerLevelPct, lvl);
            levelCostProp.set(cost);

            self.sendContentUpdates();
            ci.cancel();
            return;
        }

        // ===== COMBINE: самоцвет + самоцвет =====
        if (op == OP_COMBINE) {
            String t1 = GemData.getType(left);
            String t2 = GemData.getType(right);
            if (t1 == null || t1.isBlank()) return;
            if (t2 == null || t2.isBlank()) return;
            if (!t1.equalsIgnoreCase(t2)) return;

            int l1 = Math.max(1, GemData.getLevel(left));
            int l2 = Math.max(1, GemData.getLevel(right));

            int max = (gemsCfg.maxLevel > 0) ? gemsCfg.maxLevel : 10;
            int outLevel = (l1 == l2) ? (l1 + 1) : Math.max(l1, l2);
            if (outLevel > max) outLevel = max;

            ItemStack out = new ItemStack(ModItems.GEM);
            GemStacks.applyGemDataFromConfig(out, t1.trim().toLowerCase(Locale.ROOT), outLevel);

            self.getSlot(SLOT_RESULT).setStack(out);

            int levelForCost = Math.max(l1, l2);
            int cost = computeCost(eco.anvil.combineBase, eco.anvil.combinePerLevelPct, levelForCost);
            levelCostProp.set(cost);

            self.sendContentUpdates();
            ci.cancel();
        }
    }

    @Inject(method = "canTakeOutput", at = @At("HEAD"), cancellable = true)
    private void ggd$canTakeOutput(PlayerEntity player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        if (player == null) return;

        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;

        ItemStack left = self.getSlot(SLOT_LEFT).getStack();
        ItemStack right = self.getSlot(SLOT_RIGHT).getStack();

        int op = detectOperation(left, right);
        if (op == OP_NONE) return;

        EconomyConfig eco = EconomyConfigManager.get();
        if (eco == null || eco.anvil == null) return;

        Item currency = CurrencyItems.getCurrencyItemOrNull();
        if (currency == null) return;

        int cost;
        if (op == OP_INSERT) {
            int lvl = Math.max(1, GemData.getLevel(right));
            cost = computeCost(eco.anvil.insertBase, eco.anvil.insertPerLevelPct, lvl);
        } else {
            int l1 = Math.max(1, GemData.getLevel(left));
            int l2 = Math.max(1, GemData.getLevel(right));
            int levelForCost = Math.max(l1, l2);
            cost = computeCost(eco.anvil.combineBase, eco.anvil.combinePerLevelPct, levelForCost);
        }

        if (cost <= 0) return;

        if (!CurrencyPay.has(player, currency, cost)) {
            cir.setReturnValue(false);
            cir.cancel();

            if (!player.getEntityWorld().isClient()) {
                long t = player.getEntityWorld().getTime();
                if (t - ggd$lastNoMoneyMsgTick > 10) {
                    ggd$lastNoMoneyMsgTick = t;
                    player.sendMessage(Text.literal("Недостаточно монет: нужно " + cost + "."), true);
                }
            }
        }
    }

    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void ggd$payOnTake(PlayerEntity player, ItemStack taken, CallbackInfo ci) {
        if (player == null) return;
        if (player.getEntityWorld().isClient()) return;
        if (taken == null || taken.isEmpty()) return;

        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;

        ItemStack left = self.getSlot(SLOT_LEFT).getStack();
        ItemStack right = self.getSlot(SLOT_RIGHT).getStack();

        int op = detectOperation(left, right);
        if (op == OP_NONE) return;

        EconomyConfig eco = EconomyConfigManager.get();
        if (eco == null || eco.anvil == null) return;

        Item currency = CurrencyItems.getCurrencyItemOrNull();
        if (currency == null) return;

        int cost;
        if (op == OP_INSERT) {
            int lvl = Math.max(1, GemData.getLevel(right));
            cost = computeCost(eco.anvil.insertBase, eco.anvil.insertPerLevelPct, lvl);
        } else {
            int l1 = Math.max(1, GemData.getLevel(left));
            int l2 = Math.max(1, GemData.getLevel(right));
            int levelForCost = Math.max(l1, l2);
            cost = computeCost(eco.anvil.combineBase, eco.anvil.combinePerLevelPct, levelForCost);
        }

        if (cost > 0) {
            CurrencyPay.take(player, currency, cost);
        }
    }

    private static int detectOperation(ItemStack left, ItemStack right) {
        if (left == null || left.isEmpty()) return OP_NONE;
        if (right == null || right.isEmpty()) return OP_NONE;

        boolean leftGem = left.isOf(ModItems.GEM);
        boolean rightGem = right.isOf(ModItems.GEM);

        if (!leftGem && rightGem) return OP_INSERT;

        if (leftGem && rightGem) {
            String t1 = GemData.getType(left);
            String t2 = GemData.getType(right);
            if (t1 != null && !t1.isBlank() && t1.equalsIgnoreCase(t2)) return OP_COMBINE;
        }

        return OP_NONE;
    }

    private static int computeCost(int base, double perLevelPct, int level) {
        int lvl = Math.max(1, level);
        int b = Math.max(0, base);
        double mult = 1.0 + (Math.max(0.0, perLevelPct) * (lvl - 1));
        return Math.max(0, (int) Math.ceil(b * mult));
    }
}
