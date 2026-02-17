package su.ggd.ggd_gems_mod.quests.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * dialogs.json (optional). V1 loads it for future UI, but gameplay may ignore.
 */
public class DialogDef {
    public String id;
    public String startNodeId;
    public List<DialogNode> nodes = new ArrayList<>();

    public static class DialogNode {
        public String id;
        public String text;

        public List<DialogChoice> choices = new ArrayList<>();

        public List<Action> actionsOnEnter = new ArrayList<>();
        public List<Action> actionsOnExit = new ArrayList<>();
    }

    public static class DialogChoice {
        public String text;
        public QuestDef.Requirements requirements;
        public List<Action> actions = new ArrayList<>();
        public String nextNodeId; // null -> close
    }

    public static class Action {
        public String type;
        public Map<String, Object> params = new HashMap<>();
    }
}
