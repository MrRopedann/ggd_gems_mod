package su.ggd.ggd_gems_mod.quests.config;

import java.util.ArrayList;
import java.util.List;

public class StageDef {
    public String id;
    public String name;
    public String description;

    public List<ObjectiveDef> objectives = new ArrayList<>();

    public String dialogOnStart;
    public String dialogOnComplete;

    public QuestDef.RewardBundle rewardsOnComplete;

    // V1: linear only
    public String nextStageId;
}
