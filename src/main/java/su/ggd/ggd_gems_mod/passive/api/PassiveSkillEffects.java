package su.ggd.ggd_gems_mod.passive.api;

import java.util.*;

public final class PassiveSkillEffects {
    private PassiveSkillEffects() {}

    private static final Map<String, PassiveSkillEffect> BY_ID = new HashMap<>();

    public static void register(PassiveSkillEffect e) {
        if (e == null) return;
        String id = e.effectId();
        if (id == null || id.isBlank()) return;
        BY_ID.put(id.toLowerCase(Locale.ROOT), e);
    }

    public static PassiveSkillEffect get(String effectId) {
        if (effectId == null || effectId.isBlank()) return null;
        return BY_ID.get(effectId.toLowerCase(Locale.ROOT));
    }
}
