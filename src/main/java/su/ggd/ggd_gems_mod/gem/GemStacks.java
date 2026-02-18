package su.ggd.ggd_gems_mod.gem;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Locale;

public final class GemStacks {
    private GemStacks() {}

    public static void applyGemDataFromConfig(ItemStack stack, String type, int level) {
        if (stack == null || stack.isEmpty()) return;

        String key = (type == null ? "" : type).trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) return;

        var def = GemRegistry.get(key);

        int max = (def != null && def.maxLevel > 0) ? def.maxLevel : 10;
        int lvl = Math.max(1, Math.min(level, max));

        // ВСЕГДА записываем отличия стаков
        GemData.setType(stack, key);
        GemData.setLevel(stack, lvl);

        // имя — только если задано в конфиге; иначе используем lang "Самоцвет"
        if (def != null && def.name != null && !def.name.isBlank()) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(def.name));
        } else {
            stack.remove(DataComponentTypes.CUSTOM_NAME);
        }
    }
}
