package su.ggd.ggd_gems_mod.quests.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Quest meta (quest.json)
 * V1: minimal, expandable.
 */
public class QuestDef {
    public int formatVersion = 1;

    public String id;
    public String name;
    public String description;

    public String category;          // story/daily/guild/etc
    public String iconItem;          // item id for UI

    public boolean repeatable = false;

    public String startStageId;

    public StartRule start;          // optional, V1 may ignore
    public Requirements requirements;

    public RewardBundle rewardsOnComplete;

    public List<String> flagsOnStart;
    public List<String> flagsOnComplete;

    // ---- nested shapes ----

    public static class StartRule {
        public String type;          // AUTO / NPC / ITEM / COMMAND (V1 informational)
        public String npcId;         // for NPC start (optional)
        public String itemId;        // for ITEM start (optional)
    }

    public static class Requirements {
        public String professionIs;
        public List<String> professionIn;

        public Integer minSpentSkillPoints;
        public Integer minPlayerLevel;

        public List<String> completedQuestsAll;
        public List<String> completedQuestsAny;
        public List<String> notCompletedQuests;

        public List<ItemCount> hasItems;

        public List<String> flagsAll;
        public List<String> flagsAny;
        public List<String> flagsNot;
    }

    public static class ItemCount {
        public String itemId;
        public int count;
    }

    public static class RewardBundle {
        public List<Reward> rewards = new ArrayList<>();
    }

    public static class Reward {
        public String type; // ITEMS / XP / CURRENCY / SKILL_POINTS / SET_PROFESSION / MESSAGE ...
        public String id;   // itemId/currencyId/professionId, depends on type
        public Integer count;
        public String message; // for MESSAGE
        public String mode;    // chat/title/actionbar for MESSAGE
    }
}
