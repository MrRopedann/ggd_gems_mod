package su.ggd.ggd_gems_mod.gem;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class GemStatHandlers {

    // statId -> (stack, totalBonus) applies changes
    private static final Map<String, BiConsumer<ItemStack, Double>> HANDLERS = new HashMap<>();

    public static void register(String statId, BiConsumer<ItemStack, Double> applier) {
        if (statId == null || statId.isBlank()) return;
        if (applier == null) return;
        HANDLERS.put(statId, applier);
    }

    public static BiConsumer<ItemStack, Double> get(String statId) {
        if (statId == null) return null;
        return HANDLERS.get(statId);
    }

    private GemStatHandlers() {}
}
