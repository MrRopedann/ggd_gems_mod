package su.ggd.ggd_gems_mod.passive.effects;

import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

/**
 * "Железный желудок": основная логика обработки еды реализована в PassiveSkillHooks через UseItemCallback.
 * Этот класс нужен как зарегистрированный эффект (effectId == def.id).
 */
public final class IronStomachEffect implements PassiveSkillEffect {

    @Override
    public String effectId() {
        return "iron_stomach";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        return false;
    }
}
