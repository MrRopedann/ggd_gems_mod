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
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.gem.GemData;
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
                var cfg = GemsConfigManager.get();
                if (cfg == null) return;

                // монета
                entries.add(new ItemStack(ModItems.PIASTRE));

                if (cfg.gems == null || cfg.gems.isEmpty()) return;

                // дедуп по type (чтобы не падать при дубликатах в конфиге)
                Set<String> seenTypes = new HashSet<>();

                for (var def : cfg.gems) {
                    if (def == null || def.type == null || def.type.isBlank()) continue;

                    String type = def.type.trim().toLowerCase(Locale.ROOT);
                    if (!seenTypes.add(type)) continue; // один тип — один раз

                    ItemStack stack = new ItemStack(ModItems.GEM);

                    // ВАЖНО: сначала гарантируем уникальные компоненты (даже если конфиг сломан)
                    GemData.setType(stack, type);
                    GemData.setLevel(stack, 1);

                    // Затем применяем имя/описание из конфига (если найдётся def в индексе)
                    GemStacks.applyGemDataFromConfig(stack, type, 1);

                    // доп. защита: если почему-то данные не записались — пропускаем
                    String writtenType = GemData.getType(stack);
                    int writtenLvl = GemData.getLevel(stack);
                    if (writtenType == null || writtenType.isBlank() || writtenLvl <= 0) {
                        System.out.println("[ggd_gems_mod] skip broken gem stack for type=" + type);
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
