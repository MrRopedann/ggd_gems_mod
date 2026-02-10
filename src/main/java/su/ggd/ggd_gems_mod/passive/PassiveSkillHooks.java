package su.ggd.ggd_gems_mod.passive;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.effects.BonusHpEffect;
import su.ggd.ggd_gems_mod.passive.effects.MeleeDamageEffect;
import su.ggd.ggd_gems_mod.passive.effects.MoveSpeedEffect;
import su.ggd.ggd_gems_mod.passive.state.PassiveSkillsState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PassiveSkillHooks {
    private PassiveSkillHooks() {}

    private static final Map<UUID, Integer> BREAK_DAMAGE_BEFORE = new HashMap<>();
    private static final Map<UUID, PendingToolDamage> PENDING_TOOL_DAMAGE = new HashMap<>();

    private record PendingToolDamage(Hand hand, int prevDamage, int delayTicks) {}

    public static void init() {
        // 1) block break hooks
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return true;
            if (!(player instanceof ServerPlayerEntity sp)) return true;
            if (!(world instanceof ServerWorld sw)) return true;

            ItemStack tool = sp.getMainHandStack();
            if (!isSupportedTool(tool)) return true;

            // предохранитель на последней прочности
            PassiveSkillDispatcher.onToolUseBefore(sw, sp, tool);

            // запоминаем damage ДО действия
            BREAK_DAMAGE_BEFORE.put(sp.getUuid(), tool.getDamage());
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return;
            if (!(player instanceof ServerPlayerEntity sp)) return;
            if (!(world instanceof ServerWorld sw)) return;

            // переносим проверку урона инструмента на следующий тик
            Integer before = BREAK_DAMAGE_BEFORE.remove(sp.getUuid());
            ItemStack tool = sp.getMainHandStack();
            if (before != null && isSupportedTool(tool)) {
                // delay 1 тик, рука MAIN (main hand)
                PENDING_TOOL_DAMAGE.put(sp.getUuid(), new PendingToolDamage(Hand.MAIN_HAND, before, 1));
            }

            PassiveSkillDispatcher.onBlockBreakAfter(sw, sp, pos, state, blockEntity);
        });

        // 1.2) use-on-block (мотыга/топор/лопата и т.п.)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

            ItemStack tool = sp.getStackInHand(hand);
            if (!isSupportedTool(tool)) return ActionResult.PASS;

            // IMPORTANT: предохранитель от поломки на последней прочности (light_hand)
            PassiveSkillDispatcher.onToolUseBefore(sw, sp, tool);

            // Многие действия уменьшают прочность ПОСЛЕ callback -> проверяем на следующем тике
            PENDING_TOOL_DAMAGE.put(sp.getUuid(), new PendingToolDamage(hand, tool.getDamage(), 1));
            return ActionResult.PASS;
        });

        // 2) server tick hooks (регистрируем один раз)
        ServerTickEvents.END_SERVER_TICK.register(PassiveSkillHooks::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null || cfg.byId == null) return;

        // ===== light_hand: pending tool damage check =====
        if (!PENDING_TOOL_DAMAGE.isEmpty()) {
            Iterator<Map.Entry<UUID, PendingToolDamage>> it = PENDING_TOOL_DAMAGE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, PendingToolDamage> e = it.next();
                UUID id = e.getKey();
                PendingToolDamage p = e.getValue();

                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(id);
                if (sp == null) { it.remove(); continue; }

                if (p.delayTicks > 0) {
                    e.setValue(new PendingToolDamage(p.hand, p.prevDamage, p.delayTicks - 1));
                    continue;
                }

                it.remove();

                ItemStack tool = sp.getStackInHand(p.hand);
                if (!isSupportedTool(tool)) continue;

                int diff = tool.getDamage() - p.prevDamage;
                System.out.println("[light_hand] diff=" + diff + " tool=" + tool.getItem() + " prev=" + p.prevDamage + " now=" + tool.getDamage());

                if (diff > 0) {
                    PassiveSkillDispatcher.onToolDamaged(sp.getEntityWorld(), sp, tool, diff);
                }

            }
        }

        // ===== melee_damage =====
        PassiveSkillsConfig.PassiveSkillDef meleeDef = cfg.byId.get("melee_damage");
        if (meleeDef == null) {
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                MeleeDamageEffect.remove(sp);
            }
        } else {
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                ServerWorld world = sp.getEntityWorld();
                PassiveSkillsState st = PassiveSkillsState.get(world);

                int level = st.getLevel(sp.getUuid(), "melee_damage");
                if (level <= 0) MeleeDamageEffect.remove(sp);
                else MeleeDamageEffect.tickApply(sp, meleeDef, level);
            }
        }

        // ===== move_speed =====
        PassiveSkillsConfig.PassiveSkillDef moveDef = cfg.byId.get("move_speed");
        if (moveDef == null) {
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                MoveSpeedEffect.remove(sp);
            }
        } else {
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                ServerWorld world = sp.getEntityWorld();
                PassiveSkillsState st = PassiveSkillsState.get(world);

                int level = st.getLevel(sp.getUuid(), "move_speed");
                if (level <= 0) MoveSpeedEffect.remove(sp);
                else MoveSpeedEffect.tickApply(sp, moveDef, level);
            }
        }

        // ===== bonus_hp =====
        PassiveSkillsConfig.PassiveSkillDef hpDef = cfg.byId.get("bonus_hp");
        if (hpDef == null) {
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                BonusHpEffect.remove(sp);
            }
        } else {
            for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
                ServerWorld world = sp.getEntityWorld();
                PassiveSkillsState st = PassiveSkillsState.get(world);

                int level = st.getLevel(sp.getUuid(), "bonus_hp");
                if (level <= 0) BonusHpEffect.remove(sp);
                else BonusHpEffect.tickApply(sp, hpDef, level);
            }
        }
    }

    private static boolean isSupportedTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.isDamageable()) return false;

        return stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.SHOVELS)
                || stack.isIn(ItemTags.HOES);
    }
}
