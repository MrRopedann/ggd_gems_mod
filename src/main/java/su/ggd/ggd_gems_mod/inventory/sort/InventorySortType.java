package su.ggd.ggd_gems_mod.inventory.sort;

import java.util.Locale;

public enum InventorySortType {
    ITEM_ID,
    NAME,
    COUNT,
    RARITY;

    public static InventorySortType fromWire(String v) {
        if (v == null || v.isBlank()) return NAME;
        String s = v.trim().toUpperCase(Locale.ROOT);
        try {
            return InventorySortType.valueOf(s);
        } catch (IllegalArgumentException ignored) {
            return NAME;
        }
    }

    public String toWire() {
        return name();
    }
}
