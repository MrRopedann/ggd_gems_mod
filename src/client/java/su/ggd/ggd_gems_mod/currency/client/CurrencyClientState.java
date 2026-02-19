package su.ggd.ggd_gems_mod.currency.client;

public final class CurrencyClientState {
    private CurrencyClientState() {}

    private static volatile long balance = 0L;

    // основной API
    public static long get() { return Math.max(0L, balance); }
    public static void set(long v) { balance = Math.max(0L, v); }

    // алиасы под твой UI-код
    public static long getBalance() { return get(); }
    public static void setBalance(long v) { set(v); }
}
