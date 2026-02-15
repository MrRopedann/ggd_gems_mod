package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import su.ggd.ggd_gems_mod.mob.MobLevelClientCache;

public final class MobLevelSyncClient {
    private MobLevelSyncClient() {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(MobLevelNet.MOB_LEVEL_ID, (payload, context) -> {
            int id = payload.entityId();
            int lvl = payload.level();
            context.client().execute(() -> MobLevelClientCache.set(id, lvl));
        });
    }
}
