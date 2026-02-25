package su.ggd.ggd_gems_mod.config;

public final class MobRulesConfig {
    public boolean disableDaylightBurn = true;

    /**
     * Если true — мод будет дополнительно пытаться спавнить враждебных мобов днём.
     * Важно: это НЕ отменяет ванильный спавн ночью/в пещерах.
     */
    public boolean hostilesSpawnDaytime = true;

    /** Как часто пытаться спавнить (в тиках). 40 = ~2 секунды. */
    public int daytimeSpawnIntervalTicks = 10;

    /** Количество попыток спавна за один запуск (на мир). */
    public int daytimeSpawnAttempts = 32;

    /** Множитель лимита MONSTER (mob-cap). 1.0 = ванила, 2.0 = в 2 раза больше */
    public double monsterCapMultiplier = 1.2;

    /** Жёсткий кап (если > 0), перекрывает multiplier */
    public int monsterCapOverride = 0;

    /**
     * Если true — днём разрешаем спавн независимо от света (НЕ блокируем попытку по освещению).
     * Если false — работает ограничение daytimeMaxLight (как раньше).
     */
    public boolean daytimeIgnoreLight = true;

    /** Максимальный допустимый свет, если daytimeIgnoreLight=false (0..15). */
    public int daytimeMaxLight = 15;

    // -------------------- NEW: dark > light weighting --------------------

    /**
     * Если true — шанс успешной попытки зависит от освещения:
     * в темноте чаще, на свету реже.
     *
     * Работает только когда daytimeIgnoreLight=true,
     * либо когда daytimeIgnoreLight=false, но свет всё равно <= daytimeMaxLight.
     */
    public boolean daytimeLightWeightedChance = true;

    /**
     * Минимальный шанс успешной попытки при свете 15.
     * 0.25 = в очень светлом месте 25% попыток проходят.
     * Диапазон: 0.0..1.0
     */
    public double daytimeChanceMinAtLight15 = 0.25;

    /**
     * Шанс в полной темноте (свет 0).
     * Обычно 1.0.
     * Диапазон: 0.0..1.0
     */
    public double daytimeChanceMaxAtLight0 = 1.0;

    /**
     * “Крутизна” кривой: 1.0 линейно, 2.0 сильнее давит свет (темнота выигрывает).
     * Рекомендуемо 1.5..3.0.
     */
    public double daytimeChanceCurvePower = 2.0;
}