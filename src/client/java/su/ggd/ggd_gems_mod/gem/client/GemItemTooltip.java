package su.ggd.ggd_gems_mod.gem.client;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.gem.GemData;
import su.ggd.ggd_gems_mod.gem.GemRegistry;

import java.util.List;
import java.util.Locale;

public final class GemItemTooltip {
    private GemItemTooltip() {}

    public static void append(ItemStack stack, List<Text> tooltip) {
        String type = GemData.getType(stack);
        if (type == null || type.isBlank()) return;

        int level = GemData.getLevel(stack);
        if (level <= 0) level = 1;

        var def = GemRegistry.get(type);
        if (def == null) return;

        if (def.description != null && !def.description.isBlank()) {
            tooltip.add(Text.literal(def.description).formatted(Formatting.GRAY));
        }

        int max = (def.maxLevel > 0) ? def.maxLevel : 10;
        if (level > max) level = max;

        String targets = (def.targets == null || def.targets.isEmpty())
                ? Text.translatable("tooltip.ggd_gems_mod.target.any").getString()
                : String.join(", ", def.targets);

        Text statText = buildStatText(def.stat);

        double base = def.base;
        double per = def.perLevel;
        double total = base + per * Math.max(0, level - 1);

        tooltip.add(Text.translatable("tooltip.ggd_gems_mod.level", level, max).formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("tooltip.ggd_gems_mod.targets", targets).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.ggd_gems_mod.stat", statText).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.ggd_gems_mod.base", fmt(base)).formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.ggd_gems_mod.per_level", signed(fmt(per))).formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.ggd_gems_mod.total", signed(fmt(total))).formatted(Formatting.GREEN));
    }

    private static Text buildStatText(String statRaw) {
        if (statRaw == null || statRaw.isBlank()) return Text.literal("?");

        Identifier id = Identifier.tryParse(statRaw.trim());
        if (id == null) return Text.literal(statRaw);

        // ✅ 1.21+: attribute.minecraft.luck
        return Text.translatable("attribute." + id.getNamespace() + "." + id.getPath());
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.4f", v)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private static String signed(String s) {
        if (s.startsWith("-") || s.startsWith("+")) return s;
        return "+" + s;
    }
}
