package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import su.ggd.ggd_gems_mod.combat.DamagePopups;
import su.ggd.ggd_gems_mod.net.DamageNet;

public final class DamageIndicatorClient {
    private DamageIndicatorClient() {}

    private static boolean DONE = false;

    public static void init() {
        if (DONE) return;
        DONE = true;

        ClientPlayNetworking.registerGlobalReceiver(DamageNet.DAMAGE_INDICATOR_ID, (payload, context) -> {
            context.client().execute(() -> DamagePopups.add(payload.entityId(), payload.amount()));
        });
    }
}
