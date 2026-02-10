package su.ggd.ggd_gems_mod.passive.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.HashMap;

/**
 * Увеличение урона ближнего боя (меч/топор).
 * Реализация через AttributeModifier (без миксинов).
 *
 * Yarn 1.21+: EntityAttributeModifier = record(Identifier id, double value, Operation op)
 * => удаляем/заменяем по Identifier.
 */
public final class MeleeDamageEffect implements PassiveSkillEffect {

    private static final Identifier MOD_ADD_ID  = Identifier.of("ggd_gems_mod", "passive_melee_damage_add");
    private static final Identifier MOD_MULT_ID = Identifier.of("ggd_gems_mod", "passive_melee_damage_mult");

    @Override
    public String effectId() {
        return "melee_damage";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }

        // плоская прибавка
        JsonParamUtil.putIfMissing(def.params, "addBase", 0.0);
        JsonParamUtil.putIfMissing(def.params, "addPerLevel", 0.25);

        // опционально: мультипликатор (обычно 0)
        JsonParamUtil.putIfMissing(def.params, "multBase", 0.0);
        JsonParamUtil.putIfMissing(def.params, "multPerLevel", 0.0);
        return false;
    }

    /** вызывать на сервере в тике игрока */
    public static void tickApply(PlayerEntity player, PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (player == null || def == null || level <= 0) return;

        ItemStack main = player.getMainHandStack();
        boolean isMelee = !main.isEmpty() && (main.isIn(ItemTags.SWORDS) || main.isIn(ItemTags.AXES));

        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (inst == null) return;

        if (!isMelee) {
            remove(player);
            return;
        }

        double addBase = JsonParamUtil.getDouble(def.params, "addBase", 0.0);
        double addPer = JsonParamUtil.getDouble(def.params, "addPerLevel", 0.25);
        double multBase = JsonParamUtil.getDouble(def.params, "multBase", 0.0);
        double multPer = JsonParamUtil.getDouble(def.params, "multPerLevel", 0.0);

        double add = addBase + addPer * level;
        double mult = multBase + multPer * level;

        // обновляем: сначала удаляем старые, потом ставим новые
        inst.removeModifier(MOD_ADD_ID);
        inst.removeModifier(MOD_MULT_ID);

        if (add != 0.0) {
            inst.addPersistentModifier(new EntityAttributeModifier(
                    MOD_ADD_ID,
                    add,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (mult != 0.0) {
            inst.addPersistentModifier(new EntityAttributeModifier(
                    MOD_MULT_ID,
                    mult,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    public static void remove(PlayerEntity player) {
        EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (inst == null) return;
        inst.removeModifier(MOD_ADD_ID);
        inst.removeModifier(MOD_MULT_ID);
    }
}
