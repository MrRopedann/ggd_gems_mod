package su.ggd.ggd_gems_mod.passive;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
    private static final Map<UUID, EatTrack> IRON_STOMACH_EAT = new HashMap<>();

    private record PendingToolDamage(Hand hand, int prevDamage, int delayTicks) {}
    private record EatTrack(Hand hand, Item item, int startCount, long endGameTime) {}


    // ===== iron_stomach (food handling) =====
// Поддерживаем:
// - гнилая плоть + сырая курятина: без голода/отравления, но мало насыщают
// - сырое мясо (и рыба): можно есть без голода, без штрафов
    private static final Item[] IRON_STOMACH_FOODS = new Item[] {
            Items.ROTTEN_FLESH,
            Items.CHICKEN,
            Items.BEEF,
            Items.PORKCHOP,
            Items.MUTTON,
            Items.RABBIT,
            Items.COD,
            Items.SALMON
    };


    public static void init() {
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof net.minecraft.entity.LivingEntity victim)) return true;

            net.minecraft.entity.Entity attacker = source.getAttacker();
            if (!(attacker instanceof net.minecraft.server.network.ServerPlayerEntity sp)) return true;
            if (!(sp.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld sw)) return true;

            PassiveSkillDispatcher.onEntityDamagedByPlayer(sw, sp, victim, source, amount);
            return true;
        });


        // 1) block break hooks
        // 0) iron_stomach: разрешаем есть даже на полном голоде и убираем штрафы
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty()) return ActionResult.PASS;

            Item item = stack.getItem();
            if (!isIronStomachFood(item)) return ActionResult.PASS;

            // CLIENT: запускаем использование, чтобы появилась анимация еды
            if (world.isClient()) {
                // НИЧЕГО не форсим на клиенте — иначе будет анимация без серверного расхода
                return ActionResult.PASS;
            }


            // SERVER: тоже форсим использование (иначе при полном голоде ванилла не даст есть)
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

            int lvl = getSkillLevel(sw, sp, "iron_stomach");
            if (lvl <= 0) return ActionResult.PASS;

            EatTrack existing = IRON_STOMACH_EAT.get(sp.getUuid());

            // Если уже "едим" этот же предмет этой же рукой — НЕ перезапускаем таймер
            if (existing != null
                    && existing.hand() == hand
                    && existing.item() == item
                    && sw.getTime() < existing.endGameTime()) {

                sp.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }

            // Иначе стартуем новый цикл еды
            int useTicks = 32; // стандартная длительность еды
            long endTime = sw.getTime() + useTicks;

            IRON_STOMACH_EAT.put(sp.getUuid(), new EatTrack(hand, item, stack.getCount(), endTime));

            sp.setCurrentHand(hand);
            return ActionResult.CONSUME;


        });

        // 1.3) player damaged hooks (skin_mutation and similar)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity sp)) return true;
            if (!(sp.getEntityWorld() instanceof ServerWorld sw)) return true;

            PassiveSkillDispatcher.onPlayerDamaged(sw, sp, source, amount);
            return true;
        });


        ServerTickEvents.END_SERVER_TICK.register(PassiveSkillHooks::ironStomachTick);

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

    private static boolean hasSkill(ServerWorld world, ServerPlayerEntity player, String skillId) {
        if (world == null || player == null) return false;

        PassiveSkillsState st = PassiveSkillsState.get(world);
        if (st == null) return false;

        return st.getLevel(player.getUuid(), skillId) > 0;
    }


    private static boolean isIronStomachFood(Item item) {
        if (item == null) return false;
        for (Item it : IRON_STOMACH_FOODS) if (it == item) return true;
        return false;
    }

    private static void ironStomachTick(MinecraftServer server) {
        for (ServerPlayerEntity sp : server.getPlayerManager().getPlayerList()) {
            ServerWorld sw = sp.getEntityWorld();

            EatTrack t = IRON_STOMACH_EAT.get(sp.getUuid());
            if (t == null) continue;

            if (sw.getTime() < t.endGameTime()) continue;

            ItemStack handStack = sp.getStackInHand(t.hand());
            if (handStack.isEmpty() || handStack.getItem() != t.item()) {
                IRON_STOMACH_EAT.remove(sp.getUuid());
                continue;
            }

            // если уже как-то изменилось — не трогаем
            if (handStack.getCount() != t.startCount()) {
                IRON_STOMACH_EAT.remove(sp.getUuid());
                continue;
            }

            sp.stopUsingItem();
            consumeIronStomachFood(sp, sw, t.hand());
            sp.removeStatusEffect(StatusEffects.HUNGER);
            sp.removeStatusEffect(StatusEffects.POISON);
            IRON_STOMACH_EAT.remove(sp.getUuid());
        }
    }

    private static int getSkillLevel(ServerWorld world, ServerPlayerEntity player, String skillId) {
        PassiveSkillsState st = PassiveSkillsState.get(world);
        if (st == null) return 0;
        return st.getLevel(player.getUuid(), skillId);
    }


    private static boolean isPenaltyFood(Item item) {
        return item == Items.ROTTEN_FLESH || item == Items.CHICKEN;
    }

    private static void consumeIronStomachFood(ServerPlayerEntity player, ServerWorld world, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) return;

        Item item = stack.getItem();

        // "мало насыщает" для rotten flesh / raw chicken
        int hunger;
        float saturation;

        if (item == Items.ROTTEN_FLESH || item == Items.CHICKEN) {
            hunger = 2;
            saturation = 0.1f;
        } else if (item == Items.COD || item == Items.SALMON) {
            hunger = 2;
            saturation = 0.1f;
        } else if (item == Items.MUTTON) {
            hunger = 2;
            saturation = 0.3f;
        } else {
            // BEEF / PORKCHOP / RABBIT
            hunger = 3;
            saturation = 0.3f;
        }

        // списываем 1 предмет
        if (!player.isCreative()) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                player.setStackInHand(hand, ItemStack.EMPTY);
            }
        }

        // добавляем еду (даже при полном голоде)
        player.getHungerManager().add(hunger, saturation);

        // гарантированно чистим (на всякий)
        player.removeStatusEffect(StatusEffects.HUNGER);
        player.removeStatusEffect(StatusEffects.POISON);
    }




}
