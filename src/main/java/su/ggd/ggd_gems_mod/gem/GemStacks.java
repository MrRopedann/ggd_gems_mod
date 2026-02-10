package su.ggd.ggd_gems_mod.gem;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;

import java.util.Locale;

public final class GemStacks {
    private GemStacks() {}

    public static void applyGemDataFromConfig(ItemStack stack, String type, int level) {
        GemsConfig cfg = GemsConfigManager.get();
        if (type == null) type = "";
        String key = type.toLowerCase(Locale.ROOT);

        int max = (cfg != null && cfg.maxLevel > 0) ? cfg.maxLevel : 10;
        int lvl = Math.max(1, Math.min(level, max));

        // ВСЕГДА записываем отличия стаков
        GemData.setType(stack, key);
        GemData.setLevel(stack, lvl);

        // имя — если def есть
        GemsConfig.GemDef def = (cfg != null && cfg.byType != null) ? cfg.byType.get(key) : null;
        String displayName = (def != null && def.name != null && !def.name.isBlank())
                ? def.name
                : ("Самоцвет: " + key);

        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName));
    }


}
