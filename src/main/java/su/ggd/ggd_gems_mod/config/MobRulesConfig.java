package su.ggd.ggd_gems_mod.config;

public final class MobRulesConfig {
    public boolean disableDaylightBurn = true;

    /**
     * Если true — мод будет дополнительно пытаться спавнить враждебных мобов днём,
     * но только при выполнении обычных «тёмных» условий (уровень света 0, место и т.д.).
     *
     * Важно: это НЕ отменяет ванильный спавн ночью/в пещерах.
     */
    public boolean hostilesSpawnDaytime = true;

    /**
     * Как часто пытаться спавнить (в тиках). 40 = примерно 2 секунды.
     * Меньше значение = чаще попытки.
     */
    public int daytimeSpawnIntervalTicks = 10;

    /** Количество попыток спавна за один запуск (на мир). */
    public int daytimeSpawnAttempts = 32;

    /** Множитель лимита MONSTER (mob-cap). 1.0 = ванила, 2.0 = в 2 раза больше */
    public double monsterCapMultiplier = 1.2;

    /** Жёсткий кап (если > 0), перекрывает multiplier */
    public int monsterCapOverride = 0;

    /** Если true — днём разрешаем спавн даже при свете (игнорируем проверку light==0). */
    public boolean daytimeIgnoreLight = true;

    /** Максимальный допустимый свет, если daytimeIgnoreLight=false (0..15). */
    public int daytimeMaxLight = 15;
}
