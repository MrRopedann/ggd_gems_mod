package su.ggd.ggd_gems_mod;

import net.fabricmc.api.ClientModInitializer;
import su.ggd.ggd_gems_mod.init.client.ClientInitPlan;

public class Ggd_gems_modClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientInitPlan.init();
    }
}
