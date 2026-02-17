package su.ggd.ggd_gems_mod.quests.config;

import java.util.HashMap;
import java.util.Map;

/**
 * V1 objective shape (stages.json)
 * - target and constraints are maps to stay flexible and avoid hardcoding.
 */
public class ObjectiveDef {
    public String id;
    public String type; // TALK/KILL/BREAK_BLOCK/COLLECT_ITEM/...

    public Map<String, Object> target = new HashMap<>();
    public int required = 1;

    public Map<String, Object> constraints = new HashMap<>();

    public String uiText;
    public boolean hidden = false;
}
