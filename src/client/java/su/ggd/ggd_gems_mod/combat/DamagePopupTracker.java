package su.ggd.ggd_gems_mod.combat;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

public final class DamagePopupTracker {
    private DamagePopupTracker() {}

    private static boolean DONE = false;

    private static final double TRACK_DISTANCE = 24.0;
    private static final Map<Integer, Float> lastHp = new HashMap<>();

    public static void init() {
        if (DONE) return;
        DONE = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            lastHp.clear();
            return;
        }

        // чистим “старые” записи, если сущность пропала
        lastHp.entrySet().removeIf(e -> mc.world.getEntityById(e.getKey()) == null);

        for (LivingEntity e : mc.world.getEntitiesByClass(
                LivingEntity.class,
                mc.player.getBoundingBox().expand(TRACK_DISTANCE),
                ent -> ent.isAlive() && !(ent instanceof PlayerEntity)
        )) {
            int id = e.getId();
            float hp = e.getHealth();

            Float prev = lastHp.put(id, hp);
            if (prev == null) continue;

            float delta = prev - hp;
            if (delta > 0.01f) {
                DamagePopups.add(id, delta);
            }
        }
    }
}
