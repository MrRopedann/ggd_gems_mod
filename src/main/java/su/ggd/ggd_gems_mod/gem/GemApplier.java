package su.ggd.ggd_gems_mod.gem;

import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.function.BiConsumer;

public final class GemApplier {

    public static void apply(ItemStack stack) {
        Map<String, Double> totals = GemBonusCalculator.computeTotals(stack);
        if (totals.isEmpty()) return;

        for (var e : totals.entrySet()) {
            BiConsumer<ItemStack, Double> h = GemStatHandlers.get(e.getKey());
            if (h == null) continue;
            h.accept(stack, e.getValue());
        }
    }

    private GemApplier() {}
}
