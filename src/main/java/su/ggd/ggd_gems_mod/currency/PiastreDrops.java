package su.ggd.ggd_gems_mod.currency;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import su.ggd.ggd_gems_mod.config.EconomyConfig;
import su.ggd.ggd_gems_mod.config.EconomyConfigManager;

public final class PiastreDrops {
    private PiastreDrops() {}

    private static boolean DONE = false;

    public static void register() {
        if (DONE) return;
        DONE = true;

        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource damageSource) -> {
            if (!(entity.getEntityWorld() instanceof ServerWorld)) return;
            if (!(entity instanceof HostileEntity)) return;

            EconomyConfig cfg = EconomyConfigManager.get();
            if (cfg == null || cfg.drops == null || !cfg.drops.enabled) return;

            double chance = cfg.drops.chance;
            if (chance <= 0.0) return;

            Random random = entity.getRandom();
            if (random.nextDouble() >= chance) return;

            int min = Math.max(1, cfg.drops.min);
            int max = Math.max(min, cfg.drops.max);
            int count = (min == max) ? min : (min + random.nextInt(max - min + 1));
            if (count <= 0) return;

            Entity attacker = damageSource.getAttacker();
            if (!(attacker instanceof ServerPlayerEntity player)) return;

            CurrencyService.add(player, count);
        });
    }
}
