package su.ggd.ggd_gems_mod.config;

public final class MobRulesConfig {
    public boolean disableDaylightBurn = true;
    public boolean hostilesSpawnAnyLight = true;

    /** Множитель лимита MONSTER (mob-cap). 1.0 = ванила, 2.0 = в 2 раза больше */
    public double monsterCapMultiplier = 2.0;

    /** Жёсткий кап (если > 0), перекрывает multiplier */
    public int monsterCapOverride = 0;
}
