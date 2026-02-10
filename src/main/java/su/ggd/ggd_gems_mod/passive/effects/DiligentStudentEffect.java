package su.ggd.ggd_gems_mod.passive.effects;

import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.OnXpOrbPickup;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.HashMap;

public final class DiligentStudentEffect implements PassiveSkillEffect, OnXpOrbPickup {

    @Override public String effectId() { return "diligent_student"; }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }
        JsonParamUtil.putIfMissing(def.params, "xpBonusPercentPerLevel", 0.025);
        return false;
    }

    @Override
    public int modifyXp(ServerWorld world, ServerPlayerEntity player, int currentXp,
                        PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (currentXp <= 0) return currentXp;

        double perLevel = JsonParamUtil.getDouble(def.params, "xpBonusPercentPerLevel", 0.025);
        if (perLevel <= 0) return currentXp;

        double mult = 1.0 + (perLevel * level);
        int boosted = (int) Math.floor(currentXp * mult);
        if (boosted < currentXp) boosted = currentXp;
        return boosted;
    }
}
