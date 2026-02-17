package su.ggd.ggd_gems_mod.quests.config;

import java.util.*;

/**
 * Aggregated quests database (server in-memory; also used for sync json).
 */
public class QuestsDatabase {
    public int formatVersion = 1;

    public List<QuestFullDef> quests = new ArrayList<>();

    public transient Map<String, QuestFullDef> byId = new HashMap<>();

    public void rebuildIndex() {
        HashMap<String, QuestFullDef> m = new HashMap<>();
        for (QuestFullDef q : quests) {
            if (q == null || q.quest == null || q.quest.id == null) continue;
            m.put(q.quest.id, q);
        }
        byId = m;
    }

    public QuestFullDef get(String questId) {
        if (questId == null) return null;
        if (byId == null || byId.isEmpty()) rebuildIndex();
        return byId.get(questId);
    }

    public static class QuestFullDef {
        public QuestDef quest;
        public List<StageDef> stages = new ArrayList<>();
        public List<DialogDef> dialogs = new ArrayList<>();
    }
}
