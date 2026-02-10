package su.ggd.ggd_gems_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class WeaponStatsCommand {
    private WeaponStatsCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("weaponstats")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    ServerPlayerEntity p = src.getPlayerOrThrow();
                    var stack = p.getMainHandStack();

                    if (stack.isEmpty()) {
                        src.sendFeedback(() -> Text.literal("§cВозьми предмет в главную руку."), false);
                        return 1;
                    }

                    RegistryEntry<EntityAttribute> attackDamageAttr = attributeEntry("attack_damage");
                    if (attackDamageAttr == null) {
                        src.sendFeedback(() -> Text.literal("§cНе найден атрибут minecraft:attack_damage"), false);
                        return 1;
                    }

                    double gemBonus = 0.0;
                    double otherBonus = 0.0;

                    AttributeModifiersComponent mods = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                    if (mods != null) {
                        for (AttributeModifiersComponent.Entry e : mods.modifiers()) {
                            if (!e.attribute().equals(attackDamageAttr)) continue;
                            if (e.slot() != AttributeModifierSlot.MAINHAND) continue;

                            EntityAttributeModifier m = e.modifier();
                            if (m.operation() != EntityAttributeModifier.Operation.ADD_VALUE) continue;

                            if (isGemModifier(m.id())) gemBonus += m.value();
                            else otherBonus += m.value();
                        }
                    }

                    double itemTotalBonus = otherBonus + gemBonus;

                    // Игрок: общий total attack_damage (для сопоставления)
                    double playerTotal = 0.0;
                    EntityAttributeInstance inst = p.getAttributeInstance(attackDamageAttr);
                    if (inst != null) playerTotal = inst.getValue();

                    final double otherBonusF = otherBonus;
                    final double gemBonusF = gemBonus;
                    final double itemTotalBonusF = itemTotalBonus;
                    final double playerTotalF = playerTotal;

                    src.sendFeedback(() -> Text.literal("§6=== WeaponStats ==="), false);
                    src.sendFeedback(() -> Text.literal("§7Предмет: §f" + stack.getName().getString()), false);

                    src.sendFeedback(() -> Text.literal("§7Урон предмета (не самоцветы): §a" + fmt(otherBonusF)), false);
                    src.sendFeedback(() -> Text.literal("§7Урон от самоцветов: §d+" + fmt(gemBonusF)), false);
                    src.sendFeedback(() -> Text.literal("§7Бонус урона предмета итог: §b" + fmt(itemTotalBonusF)), false);
                    src.sendFeedback(() -> Text.literal("§7Attack Damage игрока (total): §e" + fmt(playerTotalF)), false);

                    return 1;
                })
        );
    }

    private static RegistryEntry<EntityAttribute> attributeEntry(String path) {
        Identifier id = Identifier.of("minecraft", path);
        EntityAttribute attr = Registries.ATTRIBUTE.get(id);
        if (attr == null) return null;
        return Registries.ATTRIBUTE.getEntry(attr);
    }

    private static boolean isGemModifier(Identifier id) {
        return id != null
                && "ggd_gems_mod".equals(id.getNamespace())
                && id.getPath() != null
                && id.getPath().startsWith("gem/");
    }

    private static String fmt(double v) {
        String s = String.format(Locale.US, "%.4f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
