package su.ggd.ggd_gems_mod.quests;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public final class QuestHooks {
    private QuestHooks() {}

    public static void init() {

        // KILL: фиксируем факт смерти и определяем атакующего игрока
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
            ServerPlayerEntity sp = resolveAttackingPlayer(source);
            if (sp == null) return;
            if (!(sp.getEntityWorld() instanceof ServerWorld sw)) return;

            QuestDispatcher.onKill(sw, sp, entity, source);
        });

        // DEAL_DAMAGE / TAKE_DAMAGE: используем ALLOW_DAMAGE как в PassiveSkillHooks
        // (это событие вызывается перед применением урона, нам достаточно amount)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof LivingEntity victim)) return true;

            // TAKE_DAMAGE: игрок получает урон
            if (victim instanceof ServerPlayerEntity spVictim) {
                if (!(spVictim.getEntityWorld() instanceof ServerWorld sw)) return true;
                QuestDispatcher.onTakeDamage(sw, spVictim, source, amount);
                return true;
            }

            // DEAL_DAMAGE: игрок наносит урон мобу/сущности
            ServerPlayerEntity spAttacker = resolveAttackingPlayer(source);
            if (spAttacker == null) return true;
            if (!(spAttacker.getEntityWorld() instanceof ServerWorld sw)) return true;

            QuestDispatcher.onDealDamage(sw, spAttacker, victim, source, amount);
            return true;
        });

        // BREAK_BLOCK
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return;
            if (!(player instanceof ServerPlayerEntity sp)) return;
            if (!(world instanceof ServerWorld sw)) return;

            QuestDispatcher.onBreakBlock(sw, sp, state);
        });

        // WAIT_TICKS + COLLECT_ITEM (tick)
        ServerTickEvents.END_SERVER_TICK.register(QuestHooks::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        // раз в тик: WAIT_TICKS
        // раз в 20 тиков: COLLECT_ITEM (чтобы не сканировать инвентарь слишком часто)
        for (ServerWorld sw : server.getWorlds()) {
            long time = sw.getTime();
            boolean doInventoryScan = (time % 20L) == 0L;

            for (ServerPlayerEntity sp : sw.getPlayers()) {
                QuestDispatcher.onTick(sw, sp, doInventoryScan);
            }
        }
    }

    private static @Nullable ServerPlayerEntity resolveAttackingPlayer(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity sp) return sp;

        Entity direct = source.getSource();
        if (direct instanceof ProjectileEntity proj) {
            Entity owner = proj.getOwner();
            if (owner instanceof ServerPlayerEntity sp2) return sp2;
        }
        return null;
    }
}
