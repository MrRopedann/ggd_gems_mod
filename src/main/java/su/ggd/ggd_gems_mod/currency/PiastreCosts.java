package su.ggd.ggd_gems_mod.currency;

import su.ggd.ggd_gems_mod.config.EconomyConfig;
import su.ggd.ggd_gems_mod.config.EconomyConfigManager;

public final class PiastreCosts {
    private PiastreCosts() {}

    public static int insertCost(int gemLevel) {
        EconomyConfig cfg = EconomyConfigManager.get();
        if (cfg == null || cfg.anvil == null) return 0;
        return cost(cfg.anvil.insertBase, cfg.anvil.insertPerLevelPct, gemLevel);
    }

    public static int combineCost(int inputLevel) {
        EconomyConfig cfg = EconomyConfigManager.get();
        if (cfg == null || cfg.anvil == null) return 0;
        return cost(cfg.anvil.combineBase, cfg.anvil.combinePerLevelPct, inputLevel);
    }

    private static int cost(int base, double perLevelPct, int level) {
        int lvl = Math.max(1, level);
        double m = 1.0 + (Math.max(0, lvl - 1) * Math.max(0.0, perLevelPct));
        return (int) Math.max(0, Math.round(base * m));
    }
}
