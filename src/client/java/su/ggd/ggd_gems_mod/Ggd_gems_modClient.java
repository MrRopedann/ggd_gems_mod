package su.ggd.ggd_gems_mod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import su.ggd.ggd_gems_mod.hud.ExperienceBarTextHud;
import su.ggd.ggd_gems_mod.hud.MobHealthBarHud;
import su.ggd.ggd_gems_mod.hud.XpGainPopupHud;
import su.ggd.ggd_gems_mod.inventory.sort.client.InventorySortButtons;
import su.ggd.ggd_gems_mod.inventory.sort.client.InventorySortClient;
import su.ggd.ggd_gems_mod.net.GemsConfigSyncClient;
import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.gem.GemData;
import su.ggd.ggd_gems_mod.net.MobLevelSyncClient;
import su.ggd.ggd_gems_mod.passive.BowTrajectoryRenderer;
import su.ggd.ggd_gems_mod.passive.effects.*;
import su.ggd.ggd_gems_mod.registry.ModItems;
import su.ggd.ggd_gems_mod.net.DamageIndicatorClient;
import su.ggd.ggd_gems_mod.combat.DamagePopupTracker;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffects;
import su.ggd.ggd_gems_mod.passive.PassiveSkillsScreen;
import su.ggd.ggd_gems_mod.net.PassiveSkillsSyncClient;
import java.util.List;
import java.util.Locale;

public class Ggd_gems_modClient implements ClientModInitializer {

    private static KeyBinding OPEN_STATS;
    private static KeyBinding OPEN_PASSIVES;

    @Override
    public void onInitializeClient() {
        MobLevelSyncClient.init();
        SocketedGemsTooltip.init();
        DamageIndicatorClient.init();
        MobHealthBarHud.init();
        ExperienceBarTextHud.init();
        XpGainPopupHud.init();
        DamagePopupTracker.init();


        // Синк конфига самоцветов
        GemsConfigSyncClient.init();


        // Синк пассивок (конфиг + уровни игрока)
        PassiveSkillsSyncClient.init();

        PassiveSkillEffects.register(new OakRootsEffect());
        PassiveSkillEffects.register(new LuckyMinerEffect());
        PassiveSkillEffects.register(new DiligentStudentEffect());
        PassiveSkillEffects.register(new MeleeDamageEffect());
        PassiveSkillEffects.register(new RangedDamageEffect());
        PassiveSkillEffects.register(new MoveSpeedEffect());
        PassiveSkillEffects.register(new BonusHpEffect());
        PassiveSkillEffects.register(new BowTrajectoryEffect());

        BowTrajectoryRenderer.init();

        // Сортировка инвентаря/сундуков (Shift+R переключение режима)
        InventorySortClient.init();
        InventorySortButtons.init();

        ItemTooltipCallback.EVENT.register((stack, context, tooltipType, lines) -> {
            if (stack.isOf(ModItems.GEM)) {
                appendGemTooltip(stack, lines);
            }
        });

        OPEN_STATS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.open_stats",
                GLFW.GLFW_KEY_K,
                net.minecraft.client.option.KeyBinding.Category.MISC
        ));

        OPEN_PASSIVES = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.open_passives",
                GLFW.GLFW_KEY_J,
                net.minecraft.client.option.KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_STATS.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.setScreen(new PlayerStatsScreen());
            }
            while (OPEN_PASSIVES.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.setScreen(new PassiveSkillsScreen());
            }
        });
    }

    private static void appendGemTooltip(ItemStack stack, List<Text> tooltip) {
        GemsConfig cfg = GemsConfigManager.get();
        if (cfg == null) {
            tooltip.add(Text.literal("Данные самоцветов не получены с сервера.").formatted(Formatting.RED));
            return;
        }

        String type = GemData.getType(stack);
        int level = GemData.getLevel(stack);

        if (type == null || type.isBlank()) {
            tooltip.add(Text.literal("Тип: не задан").formatted(Formatting.RED));
            return;
        }

        type = type.toLowerCase(Locale.ROOT);
        GemsConfig.GemDef def = (cfg.byType == null) ? null : cfg.byType.get(type);

        if (def == null) {
            tooltip.add(Text.literal("Неизвестный тип: " + type).formatted(Formatting.RED));
            return;
        }

        if (def.description != null && !def.description.isBlank()) {
            tooltip.add(Text.literal(def.description).formatted(Formatting.GRAY));
        }

        tooltip.add(Text.empty());

        if (level <= 0) level = 1;
        if (cfg.maxLevel > 0) level = Math.min(level, cfg.maxLevel);
        tooltip.add(Text.literal("Уровень: " + level).formatted(Formatting.YELLOW));

        if (def.targets != null && !def.targets.isEmpty()) {
            tooltip.add(Text.literal("Targets: " + String.join(", ", def.targets)).formatted(Formatting.AQUA));
        }

        if (def.stat != null && !def.stat.isBlank()) {
            tooltip.add(Text.literal("Stat: ").append(StatText.displayName(def.stat)).formatted(Formatting.AQUA));
        }

        tooltip.add(Text.literal("Base: " + format(def.base)).formatted(Formatting.GREEN));
        tooltip.add(Text.literal("PerLevel: +" + format(def.perLevel)).formatted(Formatting.GREEN));

        double total = def.base + def.perLevel * Math.max(0, (level - 1));
        tooltip.add(Text.literal("Total: " + format(total)).formatted(Formatting.DARK_GREEN));
    }

    private static String format(double v) {
        return String.format(Locale.US, "%.4f", v)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
