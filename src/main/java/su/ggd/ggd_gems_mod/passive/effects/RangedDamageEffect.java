package su.ggd.ggd_gems_mod.passive.effects;

import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.HashMap;

/**
 * Увеличение урона дальнего боя (луки / арбалеты).
 *
 * Реализация:
 * - перехват урона от projectile
 * - если стрелял игрок с навыком → усиливаем урон
 *
 * Работает корректно для:
 * - Bow
 * - Crossbow
 * - любых стрел/болтов
 */
public final class RangedDamageEffect implements PassiveSkillEffect {

    @Override
    public String effectId() {
        return "ranged_damage";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }

        // плоская прибавка к урону
        JsonParamUtil.putIfMissing(def.params, "addBase", 0.0);
        JsonParamUtil.putIfMissing(def.params, "addPerLevel", 0.5);

        // мультипликатор
        JsonParamUtil.putIfMissing(def.params, "multBase", 0.0);
        JsonParamUtil.putIfMissing(def.params, "multPerLevel", 0.05);
        return false;
    }

    /**
     * Вызывается из Damage Event.
     * Возвращает новый урон.
     */
    public static float modifyDamage(
            float baseDamage,
            DamageSource source,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    ) {
        if (level <= 0) return baseDamage;
        if (!(source.getSource() instanceof PersistentProjectileEntity projectile)) return baseDamage;

        Entity owner = projectile.getOwner();
        if (!(owner instanceof PlayerEntity player)) return baseDamage;

        // проверяем, что это именно дальнее оружие
        var weapon = player.getMainHandStack();
        if (weapon.isEmpty()) return baseDamage;
        if (!weapon.isIn(ItemTags.BOW_ENCHANTABLE) && !weapon.isIn(ItemTags.CROSSBOW_ENCHANTABLE)) {
            return baseDamage;
        }

        double addBase  = JsonParamUtil.getDouble(def.params, "addBase", 0.0);
        double addPer   = JsonParamUtil.getDouble(def.params, "addPerLevel", 0.5);
        double multBase = JsonParamUtil.getDouble(def.params, "multBase", 0.0);
        double multPer  = JsonParamUtil.getDouble(def.params, "multPerLevel", 0.05);

        double add  = addBase  + addPer  * level;
        double mult = multBase + multPer * level;

        float result = (float) (baseDamage + add);
        if (mult != 0.0) {
            result *= (1.0f + (float) mult);
        }

        return result;
    }
}
