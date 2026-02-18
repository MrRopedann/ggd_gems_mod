package su.ggd.ggd_gems_mod.passive.api;

import su.ggd.ggd_gems_mod.passive.effects.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PassiveSkillEffects {
    private PassiveSkillEffects() {}

    private static final Map<String, PassiveSkillEffect> BY_ID = new HashMap<>();
    private static boolean ALL_REGISTERED = false;

    public static void registerAll() {
        if (ALL_REGISTERED) return;
        ALL_REGISTERED = true;

        register(new BeastInstinctEffect());
        register(new BonusHpEffect());
        register(new BowTrajectoryEffect());
        register(new DiligentStudentEffect());
        register(new IronStomachEffect());
        register(new LightHandEffect());
        register(new LiterateCreatorEffect());
        register(new LuckyMinerEffect());
        register(new MeleeDamageEffect());
        register(new MoveSpeedEffect());
        register(new OakRootsEffect());
        register(new RangedDamageEffect());
        register(new SkinMutationEffect());
        register(new DamageAbsorptionEffect());
        register(new VampirismEffect());
    }

    public static void register(PassiveSkillEffect e) {
        if (e == null) return;

        String id = e.effectId();
        if (id == null || id.isBlank()) return;

        String key = id.toLowerCase(Locale.ROOT);

        // не перетираем существующий эффект
        if (BY_ID.containsKey(key)) {
            System.err.println("[ggd_gems_mod] PassiveSkillEffect duplicate id: " + key
                    + " (ignored: " + e.getClass().getName() + ")");
            return;
        }

        BY_ID.put(key, e);
    }

    public static PassiveSkillEffect get(String effectId) {
        if (effectId == null || effectId.isBlank()) return null;
        return BY_ID.get(effectId.toLowerCase(Locale.ROOT));
    }
}
