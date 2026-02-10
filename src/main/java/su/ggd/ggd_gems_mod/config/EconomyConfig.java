package su.ggd.ggd_gems_mod.config;

import su.ggd.ggd_gems_mod.config.EconomyConfig;
import su.ggd.ggd_gems_mod.config.EconomyConfigManager;


public class EconomyConfig {
    public Currency currency = new Currency();
    public AnvilCosts anvil = new AnvilCosts();
    public Drops drops = new Drops();

    public static class Currency {
        public String itemId = "ggd_gems_mod:piastre";
    }

    public static class AnvilCosts {
        public int insertBase = 5;
        public int combineBase = 8;
        public double insertPerLevelPct = 0.20;
        public double combinePerLevelPct = 0.20;
    }

    public static class Drops {
        public boolean enabled = true;
        public double chance = 0.15;
        public int min = 1;
        public int max = 2;
    }
}
