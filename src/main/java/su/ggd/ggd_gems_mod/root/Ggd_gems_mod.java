package su.ggd.ggd_gems_mod.root;

import net.fabricmc.api.ModInitializer;
import su.ggd.ggd_gems_mod.init.ServerInitPlan;

public class Ggd_gems_mod implements ModInitializer {
    public static final String MOD_ID = "ggd_gems_mod";

    @Override
    public void onInitialize() {
        ServerInitPlan.init();
    }
}
