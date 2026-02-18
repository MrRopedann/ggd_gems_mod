package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import su.ggd.ggd_gems_mod.combat.DamagePopups;

public final class DamageIndicatorClient {
    private DamageIndicatorClient() {}

    public static void handle(DamageNet.DamageIndicatorPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> DamagePopups.add(payload.entityId(), payload.amount()));
    }
}
