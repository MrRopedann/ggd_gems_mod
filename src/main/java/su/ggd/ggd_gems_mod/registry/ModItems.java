package su.ggd.ggd_gems_mod.registry;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.gem.GemItem;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class ModItems {
    private ModItems() {}

    public static final Identifier GEM_ID = Identifier.of(Ggd_gems_mod.MOD_ID, "gem");
    public static final RegistryKey<Item> GEM_KEY = RegistryKey.of(RegistryKeys.ITEM, GEM_ID);

    public static final Identifier PIASTRE_ID = Identifier.of(Ggd_gems_mod.MOD_ID, "piastre");
    public static final RegistryKey<Item> PIASTRE_KEY = RegistryKey.of(RegistryKeys.ITEM, PIASTRE_ID);

    public static final Item GEM = Registry.register(
            Registries.ITEM,
            GEM_ID,
            new GemItem(new Item.Settings()
                    .registryKey(GEM_KEY)
                    .maxCount(1)
            )
    );

    public static final Item PIASTRE = Registry.register(
            Registries.ITEM,
            PIASTRE_ID,
            new Item(new Item.Settings()
                    .registryKey(PIASTRE_KEY)
                    .maxCount(64)
            )
    );

    public static void initialize() {}
}
