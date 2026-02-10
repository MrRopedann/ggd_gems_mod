package su.ggd.ggd_gems_mod.gem;

import net.minecraft.item.ItemStack;
import su.ggd.ggd_gems_mod.registry.ModDataComponents;

public final class GemData {
    private GemData() {}

    public static void set(ItemStack stack, String type, int level) {
        // гарантируем что компоненты уже инициализированы
        if (ModDataComponents.GEM_TYPE == null || ModDataComponents.GEM_LEVEL == null) {
            throw new IllegalStateException("ModDataComponents not initialized. Call ModDataComponents.initialize() in onInitialize().");
        }

        stack.set(ModDataComponents.GEM_TYPE, type);
        stack.set(ModDataComponents.GEM_LEVEL, level);
    }

    public static String getType(ItemStack stack) {
        if (ModDataComponents.GEM_TYPE == null) return null;
        return stack.get(ModDataComponents.GEM_TYPE); // может быть null — это нормально
    }


    public static int getLevel(ItemStack stack) {
        if (ModDataComponents.GEM_LEVEL == null) return 0;
        Integer v = stack.get(ModDataComponents.GEM_LEVEL);
        return v == null ? 0 : v;
    }

    public static void setType(ItemStack stack, String type) {
        if (stack == null) return;
        if (type == null) type = "";
        stack.set(ModDataComponents.GEM_TYPE, type);
    }

    public static void setLevel(ItemStack stack, int level) {
        if (stack == null) return;
        stack.set(ModDataComponents.GEM_LEVEL, level);
    }

}
