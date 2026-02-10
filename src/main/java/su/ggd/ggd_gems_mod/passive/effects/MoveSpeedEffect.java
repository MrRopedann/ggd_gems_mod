package su.ggd.ggd_gems_mod.passive.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.HashMap;

/**
 * Пассивка: скорость передвижения.
 * AttributeModifier по Identifier (Yarn 1.21+).
 */
public final class MoveSpeedEffect implements PassiveSkillEffect {

    private static final Identifier MOD_ID = Identifier.of("ggd_gems_mod", "passive_move_speed");

    @Override
    public String effectId() {
        return "move_speed";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }

        // +% скорости за уровень (например 0.02 = +2%/lvl)
        JsonParamUtil.putIfMissing(def.params, "addPerLevel", 0.02);

        // опционально, если нужно: базовый % бонус
        JsonParamUtil.putIfMissing(def.params, "addBase", 0.0);
        return false;
    }

    /** вызывать на сервере каждый тик */
    public static void tickApply(PlayerEntity player, PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (player == null) return;

        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (inst == null) return;

        // всегда сначала чистим старое (чтобы не копилось / корректно обновлялось)
        inst.removeModifier(MOD_ID);

        if (def == null || level <= 0) return;

        double addBase = JsonParamUtil.getDouble(def.params, "addBase", 0.0);
        double addPerLevel = JsonParamUtil.getDouble(def.params, "addPerLevel", 0.02);

        double total = addBase + addPerLevel * level;
        if (total == 0.0) return;

        inst.addPersistentModifier(new EntityAttributeModifier(
                MOD_ID,
                total,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    public static void remove(PlayerEntity player) {
        if (player == null) return;
        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (inst == null) return;
        inst.removeModifier(MOD_ID);
    }
}
