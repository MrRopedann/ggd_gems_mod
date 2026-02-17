package su.ggd.ggd_gems_mod.quests.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.quests.config.*;
import su.ggd.ggd_gems_mod.quests.net.QuestStateNet;
import su.ggd.ggd_gems_mod.quests.state.QuestsState;
import su.ggd.ggd_gems_mod.quests.reward.QuestRewards;
import su.ggd.ggd_gems_mod.quests.req.QuestRequirements;

import java.util.HashMap;
import java.util.Map;

public final class QuestService {
    private QuestService() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static QuestsState state(ServerWorld world) {
        return QuestsState.get(world);
    }

    public static void sync(ServerPlayerEntity player) {
        if (player == null) return;
        ServerWorld w = (ServerWorld) player.getEntityWorld();
        QuestsState st = state(w);
        QuestsState.PlayerQuestData pd = st.getOrCreate(player.getUuid());

        String json = GSON.toJson(pd);
        QuestStateNet.sendPlayerState(player, json);
    }

    public static boolean startQuest(ServerPlayerEntity player, String questId) {
        if (player == null || questId == null || questId.isBlank()) return false;

        QuestsDatabase.QuestFullDef def = QuestsConfigManager.get().get(questId);
        if (def == null || def.quest == null) return false;

        ServerWorld w = (ServerWorld) player.getEntityWorld();
        QuestsState st = state(w);
        QuestsState.PlayerQuestData pd = st.getOrCreate(player.getUuid());

        // already active
        if (pd.activeQuests.containsKey(questId)) return false;

        // if not repeatable and already completed
        if (!def.quest.repeatable && pd.completedQuests.contains(questId)) return false;

        String startStageId = def.quest.startStageId;
        if (startStageId == null || startStageId.isBlank()) return false;

        StageDef stage = findStage(def, startStageId);
        if (stage == null) return false;

        if (!QuestRequirements.canStart(def.quest, pd)) return false;

        // flagsOnStart (V1: "true")
        if (def.quest.flagsOnStart != null) {
            for (String f : def.quest.flagsOnStart) {
                if (f == null || f.isBlank()) continue;
                pd.flags.put(f, "true");
            }
        }


        QuestsState.ActiveQuestProgress pr = new QuestsState.ActiveQuestProgress(questId, startStageId);
        pr.stageStartedAtTime = w.getTime();
        pr.lastUpdateTime = w.getTime();

        // init objective progress = 0 for current stage
        for (ObjectiveDef o : stage.objectives) {
            if (o == null || o.id == null) continue;
            pr.objectives.put(o.id, 0);
        }

        pd.activeQuests.put(questId, pr);

        // default track if none
        if (pd.trackedQuestId == null) pd.trackedQuestId = questId;

        st.markDirty();
        sync(player);
        return true;
    }

    public static boolean abandonQuest(ServerPlayerEntity player, String questId) {
        if (player == null || questId == null || questId.isBlank()) return false;

        ServerWorld w = (ServerWorld) player.getEntityWorld();
        QuestsState st = state(w);
        QuestsState.PlayerQuestData pd = st.getOrCreate(player.getUuid());

        if (pd.activeQuests.remove(questId) == null) return false;

        if (questId.equals(pd.trackedQuestId)) pd.trackedQuestId = null;

        st.markDirty();
        sync(player);
        return true;
    }

    public static boolean trackQuest(ServerPlayerEntity player, String questIdOrNull) {
        if (player == null) return false;

        ServerWorld w = (ServerWorld) player.getEntityWorld();
        QuestsState st = state(w);
        QuestsState.PlayerQuestData pd = st.getOrCreate(player.getUuid());

        if (questIdOrNull == null || questIdOrNull.isBlank() || "none".equalsIgnoreCase(questIdOrNull)) {
            pd.trackedQuestId = null;
            st.markDirty();
            sync(player);
            return true;
        }

        // can track only active for now
        if (!pd.activeQuests.containsKey(questIdOrNull)) return false;

        pd.trackedQuestId = questIdOrNull;
        st.markDirty();
        sync(player);
        return true;
    }

    private static StageDef findStage(QuestsDatabase.QuestFullDef def, String stageId) {
        if (def == null || def.stages == null) return null;
        for (StageDef s : def.stages) {
            if (s != null && stageId.equals(s.id)) return s;
        }
        return null;
    }

    public static boolean advanceStageOrComplete(ServerWorld world,
                                                 ServerPlayerEntity player,
                                                 QuestsState.PlayerQuestData pd,
                                                 QuestsState.ActiveQuestProgress pr,
                                                 QuestsDatabase.QuestFullDef qdef,
                                                 StageDef currentStage) {
        if (world == null || player == null || pd == null || pr == null || qdef == null) return false;

        String nextStageId = (currentStage != null) ? currentStage.nextStageId : null;

        // Если следующей стадии нет — квест завершён
        if (nextStageId == null || nextStageId.isBlank()) {
            pd.activeQuests.remove(pr.questId);
            pd.completedQuests.add(pr.questId);

            if (pr.questId.equals(pd.trackedQuestId)) {
                pd.trackedQuestId = null;
            }
            // stage reward
            if (currentStage != null && currentStage.rewardsOnComplete != null) {
                QuestRewards.give(player, currentStage.rewardsOnComplete);
            }
            // quest complete reward
            if (qdef.quest != null && qdef.quest.rewardsOnComplete != null) {
                QuestRewards.give(player, qdef.quest.rewardsOnComplete);
            }

            player.sendMessage(net.minecraft.text.Text.literal("[Quests] Completed quest: " + pr.questId));
            // flagsOnComplete (V1: "true")
            if (qdef.quest != null && qdef.quest.flagsOnComplete != null) {
                for (String f : qdef.quest.flagsOnComplete) {
                    if (f == null || f.isBlank()) continue;
                    pd.flags.put(f, "true");
                }
            }

            return true;
        }

        // Переход на следующую стадию
        StageDef next = null;
        for (StageDef s : qdef.stages) {
            if (s != null && nextStageId.equals(s.id)) {
                next = s;
                break;
            }
        }
        if (next == null) {
            // если конфиг битый — завершаем квест, чтобы не сломать состояние
            pd.activeQuests.remove(pr.questId);
            pd.completedQuests.add(pr.questId);
            if (pr.questId.equals(pd.trackedQuestId)) pd.trackedQuestId = null;

            player.sendMessage(net.minecraft.text.Text.literal("[Quests] Completed quest (missing next stage): " + pr.questId));
            return true;
        }

        pr.currentStageId = nextStageId;
        pr.objectives.clear();
        for (ObjectiveDef o : next.objectives) {
            if (o == null || o.id == null) continue;
            pr.objectives.put(o.id, 0);
        }
        pr.stageStartedAtTime = world.getTime();
        pr.lastUpdateTime = world.getTime();

        player.sendMessage(net.minecraft.text.Text.literal("[Quests] Stage advanced: " + pr.questId + " -> " + nextStageId));
        return true;
    }

}
