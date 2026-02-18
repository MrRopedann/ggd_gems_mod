package su.ggd.ggd_gems_mod.gem;

import su.ggd.ggd_gems_mod.socket.GemSocketing;
import su.ggd.ggd_gems_mod.socket.SocketedGem;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GemBonusCalculator {

    public static Map<String, Double> computeTotals(ItemStack stack) {
        List<SocketedGem> gems = GemSocketing.getSocketed(stack);
        if (gems.isEmpty()) return Map.of();

        HashMap<String, Double> totals = new HashMap<>();

        for (SocketedGem g : gems) {
            var def = GemRegistry.get(g.type());
            if (def == null) continue;
            if (def.stat == null || def.stat.isBlank()) continue;

            int lvl = Math.max(1, g.level());
            double bonus = def.base + def.perLevel * Math.max(0, lvl - 1);

            totals.merge(def.stat, bonus, Double::sum);
        }

        return totals;
    }

    private GemBonusCalculator() {}
}
