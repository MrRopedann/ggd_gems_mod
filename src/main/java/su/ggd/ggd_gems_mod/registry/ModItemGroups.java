package su.ggd.ggd_gems_mod.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.gem.GemData;
import su.ggd.ggd_gems_mod.gem.GemRegistry;
import su.ggd.ggd_gems_mod.gem.GemStacks;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ModItemGroups {
    private ModItemGroups() {}

    public static final RegistryKey<ItemGroup> GEMS_GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(Ggd_gems_mod.MOD_ID, "gems"));

    public static final ItemGroup GEMS_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.GEM))
            .displayName(Text.translatable("itemGroup." + Ggd_gems_mod.MOD_ID + ".gems"))
            .entries((context, entries) -> {

                // монета
                entries.add(new ItemStack(ModItems.PIASTRE));

                var all = GemRegistry.all();
                if (all.isEmpty()) return;

                // дедуп по ключу (на всякий случай)
                Set<String> seen = new HashSet<>();

                for (var e : all.entrySet()) {
                    String type = e.getKey();
                    if (type == null || type.isBlank()) continue;

                    String key = type.trim().toLowerCase(Locale.ROOT);
                    if (!seen.add(key)) continue;

                    ItemStack stack = new ItemStack(ModItems.GEM);

                    // гарантируем компоненты
                    GemData.setType(stack, key);
                    GemData.setLevel(stack, 1);

                    // имя из GemRegistry (внутри GemStacks)
                    GemStacks.applyGemDataFromConfig(stack, key, 1);

                    // защита от битых данных
                    String writtenType = GemData.getType(stack);
                    int writtenLvl = GemData.getLevel(stack);
                    if (writtenType == null || writtenType.isBlank() || writtenLvl <= 0) {
                        continue;
                    }

                    entries.add(stack);
                }
            })
            .build();

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, GEMS_GROUP_KEY, GEMS_GROUP);
    }
}
