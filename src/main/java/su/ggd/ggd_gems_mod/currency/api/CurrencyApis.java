package su.ggd.ggd_gems_mod.currency.api;

import su.ggd.ggd_gems_mod.currency.impl.CurrencyApiImpl;

/**
 * Точка доступа к CurrencyApi.
 * Другие моды делают: CurrencyApis.get().add(player, 100);
 */
public final class CurrencyApis {
    private CurrencyApis() {}

    private static final CurrencyApi INSTANCE = new CurrencyApiImpl();

    public static CurrencyApi get() {
        return INSTANCE;
    }
}