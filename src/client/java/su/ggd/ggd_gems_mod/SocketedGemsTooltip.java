package su.ggd.ggd_gems_mod;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import su.ggd.ggd_gems_mod.socket.GemSocketing;
import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.socket.SocketedGem;
import su.ggd.ggd_gems_mod.StatText;


import java.util.List;
import java.util.Locale;

public final class SocketedGemsTooltip {
    private SocketedGemsTooltip() {}

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, ctx, type, tooltip) -> appendSocketInfo(stack, tooltip));
    }

    private static void appendSocketInfo(ItemStack stack, List<Text> tooltip) {
        List<SocketedGem> gems = GemSocketing.getSocketed(stack);
        if (gems.isEmpty()) return;

       /*
       tooltip.add(Text.empty());
        tooltip.add(Text.literal("Самоцветы: " + gems.size() + "/" + GemSocketing.MAX_SOCKETS)
                .formatted(Formatting.GOLD));
                */

        GemsConfig cfg = GemsConfigManager.get();

        for (int i = 0; i < gems.size(); i++) {
            SocketedGem g = gems.get(i);

            String gemType = g.type();
            int level = g.level();

            GemsConfig.GemDef def = (cfg != null && cfg.byType != null) ? cfg.byType.get(gemType) : null;

            Text gemName = Text.literal((def != null && def.name != null && !def.name.isBlank()) ? def.name : gemType);

            double base = (def != null) ? def.base : 0.0;
            double per = (def != null) ? def.perLevel : 0.0;
            double total = base + per * Math.max(0, level - 1);

            Text statName = (def != null && def.stat != null && !def.stat.isBlank())
                    ? StatText.displayName(def.stat)
                    : Text.literal("?");

            tooltip.add(Text.literal("Слот " + (i + 1) + ": ")
                    .append(gemName)
                    .append(Text.literal(" (ур. " + level + ")"))
                    .formatted(Formatting.YELLOW));

            tooltip.add(
                    Text.literal("↳ ")
                            .append(statName)
                            .append(Text.literal(": +" + format(total)
                                    + " (Базовая " + format(base) + ", +" + format(per) + "/ур.)"))
                            .formatted(Formatting.DARK_GREEN)
            );
        }

    }

    private static String format(double v) {
        return String.format(Locale.US, "%.4f", v)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
