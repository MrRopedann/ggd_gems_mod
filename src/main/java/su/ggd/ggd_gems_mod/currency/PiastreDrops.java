package su.ggd.ggd_gems_mod.currency;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
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
            if (!(entity.getEntityWorld() instanceof ServerWorld world)) return;
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

            if (cfg.currency == null || cfg.currency.itemId == null) return;
            Identifier itemId = Identifier.tryParse(cfg.currency.itemId);
            if (itemId == null) return;

            var item = Registries.ITEM.get(itemId);
            if (item == null || item == net.minecraft.item.Items.AIR) return;

            entity.dropStack(world, new ItemStack(item, count));
        });
    }
}
