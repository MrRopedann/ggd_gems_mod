package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.HashMap;

public final class BonusHpEffect implements PassiveSkillEffect {

    private static final Identifier MOD_ID = Identifier.of("ggd_gems_mod", "passive_bonus_hp");
    private static final double HP_PER_HEART = 2.0;

    @Override
    public String effectId() {
        return "bonus_hp";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }

        // дефолт
        if (!def.params.containsKey("heartsPerLevel")) {
            def.params.put("heartsPerLevel", 1.0);
            changed = true;
        }

        return changed;
    }

    /* вызывать на сервере каждый тик */
    public static void tickApply(PlayerEntity player, PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (player == null) return;

        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (inst == null) return;

        float oldMax = player.getMaxHealth();

        inst.removeModifier(MOD_ID);

        if (def == null || level <= 0) {
            clampCurrentHealth(player);
            return;
        }

        double heartsPerLevel = JsonParamUtil.getDouble(def.params, "heartsPerLevel", 1.0);
        double addHp = level * heartsPerLevel * HP_PER_HEART;

        if (addHp != 0.0) {
            inst.addPersistentModifier(new EntityAttributeModifier(
                    MOD_ID,
                    addHp,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        float newMax = player.getMaxHealth();

        float delta = newMax - oldMax;
        if (delta > 0.0001f) {
            float healed = Math.min(newMax, player.getHealth() + delta);
            player.setHealth(healed);
        } else {
            clampCurrentHealth(player);
        }
    }

    public static void remove(PlayerEntity player) {
        if (player == null) return;
        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(MOD_ID);
        clampCurrentHealth(player);
    }

    private static void clampCurrentHealth(PlayerEntity player) {
        float max = player.getMaxHealth();
        if (player.getHealth() > max) {
            player.setHealth(max);
        }
    }
}
