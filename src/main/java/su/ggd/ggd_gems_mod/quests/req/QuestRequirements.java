package su.ggd.ggd_gems_mod.quests.req;

import su.ggd.ggd_gems_mod.quests.config.QuestDef;
import su.ggd.ggd_gems_mod.quests.state.QuestsState;

import java.util.List;
import java.util.Set;

public final class QuestRequirements {
    private QuestRequirements() {}

    public static boolean canStart(QuestDef quest, QuestsState.PlayerQuestData pd) {
        if (quest == null) return false;
        QuestDef.Requirements r = quest.requirements;
        if (r == null) return true;
        if (pd == null) return false;

        // completed all
        if (r.completedQuestsAll != null && !r.completedQuestsAll.isEmpty()) {
            for (String q : r.completedQuestsAll) {
                if (q == null || q.isBlank()) continue;
                if (!pd.completedQuests.contains(q)) return false;
            }
        }

        // completed any
        if (r.completedQuestsAny != null && !r.completedQuestsAny.isEmpty()) {
            boolean ok = false;
            for (String q : r.completedQuestsAny) {
                if (q == null || q.isBlank()) continue;
                if (pd.completedQuests.contains(q)) { ok = true; break; }
            }
            if (!ok) return false;
        }

        // not completed
        if (r.notCompletedQuests != null && !r.notCompletedQuests.isEmpty()) {
            for (String q : r.notCompletedQuests) {
                if (q == null || q.isBlank()) continue;
                if (pd.completedQuests.contains(q)) return false;
            }
        }

        // flags
        if (!flagsAll(pd, r.flagsAll)) return false;
        if (!flagsAny(pd, r.flagsAny)) return false;
        if (hasAnyFlag(pd, r.flagsNot)) return false;

        return true;
    }

    private static boolean flagsAll(QuestsState.PlayerQuestData pd, List<String> flags) {
        if (flags == null || flags.isEmpty()) return true;
        for (String f : flags) {
            if (f == null || f.isBlank()) continue;
            String v = pd.flags.get(f);
            if (v == null) return false;
            if ("false".equalsIgnoreCase(v) || "0".equals(v)) return false;
        }
        return true;
    }

    private static boolean flagsAny(QuestsState.PlayerQuestData pd, List<String> flags) {
        if (flags == null || flags.isEmpty()) return true;
        for (String f : flags) {
            if (f == null || f.isBlank()) continue;
            String v = pd.flags.get(f);
            if (v == null) continue;
            if (!"false".equalsIgnoreCase(v) && !"0".equals(v)) return true;
        }
        return false;
    }

    private static boolean hasAnyFlag(QuestsState.PlayerQuestData pd, List<String> flags) {
        if (flags == null || flags.isEmpty()) return false;
        for (String f : flags) {
            if (f == null || f.isBlank()) continue;
            String v = pd.flags.get(f);
            if (v == null) continue;
            if (!"false".equalsIgnoreCase(v) && !"0".equals(v)) return true;
        }
        return false;
    }
}
