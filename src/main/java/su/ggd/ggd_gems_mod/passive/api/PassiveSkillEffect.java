package su.ggd.ggd_gems_mod.passive.api;

import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public interface PassiveSkillEffect {
    /** ключ эффекта, который указан в конфиге (def.effect) */
    String effectId();

    /**
     * поставить дефолты/нормализовать def.params + собрать runtime кэши
     *
     * @return
     */
    boolean normalize(PassiveSkillsConfig.PassiveSkillDef def);
}
