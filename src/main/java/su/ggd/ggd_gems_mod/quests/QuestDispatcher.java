package su.ggd.ggd_gems_mod.quests;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.quests.config.ObjectiveDef;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.quests.config.QuestsDatabase;
import su.ggd.ggd_gems_mod.quests.config.StageDef;
import su.ggd.ggd_gems_mod.quests.service.QuestService;
import su.ggd.ggd_gems_mod.quests.state.QuestsState;

import java.util.Map;

public final class QuestDispatcher {
    private QuestDispatcher() {}

    // ---------------- events ----------------

    public static void onKill(ServerWorld world, ServerPlayerEntity player, LivingEntity victim, DamageSource source) {
        handle(world, player, "KILL", (obj) -> matchesEntityObjective(obj, victim));
    }

    public static void onBreakBlock(ServerWorld world, ServerPlayerEntity player, BlockState state) {
        handle(world, player, "BREAK_BLOCK", (obj) -> matchesBlockObjective(obj, state));
    }

    public static void onDealDamage(ServerWorld world, ServerPlayerEntity player, LivingEntity victim, DamageSource source, float amount) {
        int add = amountToProgress(amount);
        if (add <= 0) return;
        handleAdd(world, player, "DEAL_DAMAGE", add, (obj) -> true);
    }

    public static void onTakeDamage(ServerWorld world, ServerPlayerEntity player, DamageSource source, float amount) {
        int add = amountToProgress(amount);
        if (add <= 0) return;
        handleAdd(world, player, "TAKE_DAMAGE", add, (obj) -> true);
    }

    public static void onTick(ServerWorld world, ServerPlayerEntity player, boolean doInventoryScan) {
        // WAIT_TICKS: +1 every tick for objectives
        handleAdd(world, player, "WAIT_TICKS", 1, (obj) -> true);

        // COLLECT_ITEM: rescan inventory every 20 ticks and set progress to itemCount (clamped)
        if (doInventoryScan) {
            handleCollectItem(world, player);
        }
    }

    // ---------------- core loop ----------------

    private interface ObjectivePredicate {
        boolean test(ObjectiveDef obj);
    }

    private static void handle(ServerWorld world, ServerPlayerEntity player, String type, ObjectivePredicate pred) {
        handleAdd(world, player, type, 1, pred);
    }

    private static void handleAdd(ServerWorld world, ServerPlayerEntity player, String type, int add, ObjectivePredicate pred) {
        if (player == null) return;

        QuestsDatabase db = QuestsConfigManager.get();
        if (db == null || db.byId == null) db.rebuildIndex();

        QuestsState st = QuestsState.get(world);
        QuestsState.PlayerQuestData pd = st.getOrCreate(player.getUuid());
        if (pd.activeQuests.isEmpty()) return;

        boolean changed = false;

        for (QuestsState.ActiveQuestProgress pr : pd.activeQuests.values()) {
            QuestsDatabase.QuestFullDef qdef = db.get(pr.questId);
            if (qdef == null) continue;

            StageDef stage = findStage(qdef, pr.currentStageId);
            if (stage == null) continue;

            for (ObjectiveDef obj : stage.objectives) {
                if (obj == null || obj.id == null) continue;
                if (obj.type == null) continue;

                if (!type.equalsIgnoreCase(obj.type)) continue;
                if (!pred.test(obj)) continue;

                int cur = pr.objectives.getOrDefault(obj.id, 0);
                int next = Math.min(obj.required, cur + add);
                if (next != cur) {
                    pr.objectives.put(obj.id, next);
                    pr.lastUpdateTime = world.getTime();
                    changed = true;
                }
            }

            // stage completion?
            if (changed && isStageCompleted(stage, pr.objectives)) {
                boolean stageChanged = QuestService.advanceStageOrComplete(world, player, pd, pr, qdef, stage);
                if (stageChanged) {
                    st.markDirty();
                    QuestService.sync(player);
                    // после смены стадии продолжаем цикл: текущий pr уже обновлён
                }
            }
        }

        if (changed) {
            st.markDirty();
            QuestService.sync(player);
        }
    }

    private static void handleCollectItem(ServerWorld world, ServerPlayerEntity player) {
        QuestsDatabase db = QuestsConfigManager.get();
        if (db == null || db.byId == null) db.rebuildIndex();

        QuestsState st = QuestsState.get(world);
        QuestsState.PlayerQuestData pd = st.getOrCreate(player.getUuid());
        if (pd.activeQuests.isEmpty()) return;

        boolean changed = false;

        for (QuestsState.ActiveQuestProgress pr : pd.activeQuests.values()) {
            QuestsDatabase.QuestFullDef qdef = db.get(pr.questId);
            if (qdef == null) continue;

            StageDef stage = findStage(qdef, pr.currentStageId);
            if (stage == null) continue;

            for (ObjectiveDef obj : stage.objectives) {
                if (obj == null || obj.id == null) continue;
                if (obj.type == null) continue;
                if (!"COLLECT_ITEM".equalsIgnoreCase(obj.type)) continue;

                String itemId = getString(obj.target, "itemId");
                if (itemId == null) itemId = getString(obj.target, "item"); // fallback
                if (itemId == null) continue;

                Item item = resolveItem(itemId);
                if (item == null) continue;

                int count = countItem(player, item);
                int next = Math.min(obj.required, count);
                int cur = pr.objectives.getOrDefault(obj.id, 0);

                if (next != cur) {
                    pr.objectives.put(obj.id, next);
                    pr.lastUpdateTime = world.getTime();
                    changed = true;
                }
            }

            if (changed && isStageCompleted(stage, pr.objectives)) {
                boolean stageChanged = QuestService.advanceStageOrComplete(world, player, pd, pr, qdef, stage);
                if (stageChanged) {
                    st.markDirty();
                    QuestService.sync(player);
                }
            }
        }

        if (changed) {
            st.markDirty();
            QuestService.sync(player);
        }
    }

    // ---------------- helpers ----------------

    private static boolean isStageCompleted(StageDef stage, Map<String, Integer> progress) {
        for (ObjectiveDef obj : stage.objectives) {
            if (obj == null || obj.id == null) continue;
            int cur = progress.getOrDefault(obj.id, 0);
            if (cur < Math.max(1, obj.required)) return false;
        }
        return true;
    }

    private static StageDef findStage(QuestsDatabase.QuestFullDef def, String stageId) {
        if (def == null || def.stages == null) return null;
        for (StageDef s : def.stages) {
            if (s != null && stageId != null && stageId.equals(s.id)) return s;
        }
        return null;
    }

    private static boolean matchesEntityObjective(ObjectiveDef obj, LivingEntity victim) {
        String typeId = getString(obj.target, "entityType");
        if (typeId == null) typeId = getString(obj.target, "entity");
        if (typeId == null) return true; // если не задано — любой

        Identifier id = safeId(typeId);
        if (id == null) return false;

        Identifier victimId = Registries.ENTITY_TYPE.getId(victim.getType());
        return victimId != null && victimId.equals(id);
    }

    private static boolean matchesBlockObjective(ObjectiveDef obj, BlockState state) {
        String blockId = getString(obj.target, "blockId");
        if (blockId == null) blockId = getString(obj.target, "block");
        if (blockId == null) return true;

        Identifier id = safeId(blockId);
        if (id == null) return false;

        Identifier actual = Registries.BLOCK.getId(state.getBlock());
        return actual != null && actual.equals(id);
    }

    private static int amountToProgress(float amount) {
        if (amount <= 0f) return 0;
        // V1: храним прогресс int, округляем вверх, чтобы мелкий урон тоже учитывался
        return (int) Math.ceil(amount);
    }

    private static String getString(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object v = map.get(key);
        if (v == null) return null;
        if (v instanceof String s) return s;
        return String.valueOf(v);
    }

    private static Identifier safeId(String s) {
        try {
            return Identifier.of(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static Item resolveItem(String itemId) {
        Identifier id = safeId(itemId);
        if (id == null) return null;

        if (!Registries.ITEM.containsId(id)) return null;

        return Registries.ITEM.get(id);
    }


    private static int countItem(ServerPlayerEntity player, Item item) {
        int found = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (!st.isEmpty() && st.isOf(item)) {
                found += st.getCount();
            }
        }
        return found;
    }

    // debug helper (не обязательно, но полезно)
    static void notify(ServerPlayerEntity player, String msg) {
        if (player == null) return;
        player.sendMessage(Text.literal(msg));
    }

    public static void onTalk(ServerWorld world, ServerPlayerEntity player, String npcId) {
        handle(world, player, "TALK", (obj) -> {
            String need = getString(obj.target, "npcId");
            if (need == null) need = getString(obj.target, "npc");
            if (need == null) return true; // если не задано — любой npc
            return need.equalsIgnoreCase(npcId);
        });
    }

}
