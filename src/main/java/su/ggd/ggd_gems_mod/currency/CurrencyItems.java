package su.ggd.ggd_gems_mod.currency;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.config.EconomyConfig;
import su.ggd.ggd_gems_mod.config.EconomyConfigManager;

public final class CurrencyItems {
    private CurrencyItems() {}

    public static Item getCurrencyItemOrNull() {
        EconomyConfig cfg = EconomyConfigManager.get();
        if (cfg == null || cfg.currency == null) return null;

        String raw = cfg.currency.itemId;
        if (raw == null || raw.isBlank()) return null;

        Identifier id = Identifier.tryParse(raw.trim());
        if (id == null) return null;

        Item item = Registries.ITEM.get(id);
        if (item == null || item == Items.AIR) return null;

        return item;
    }
}
