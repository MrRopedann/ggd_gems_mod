package su.ggd.ggd_gems_mod.passive.effects;

import com.google.gson.JsonObject;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.HashMap;

public final class BowTrajectoryEffect implements PassiveSkillEffect {

    @Override
    public String effectId() {
        return "bow_trajectory";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }
        if (def.maxLevel <= 0) def.maxLevel = 1;

        JsonParamUtil.putIfMissing(def.params, "steps", 60);
        JsonParamUtil.putIfMissing(def.params, "drag", 0.99);
        JsonParamUtil.putIfMissing(def.params, "gravity", 0.05);
        JsonParamUtil.putIfMissing(def.params, "minPull", 0.10);
        return false;
    }
}
