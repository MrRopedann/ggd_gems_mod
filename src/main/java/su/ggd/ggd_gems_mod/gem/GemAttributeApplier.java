package su.ggd.ggd_gems_mod.gem;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.socket.GemSocketing;
import su.ggd.ggd_gems_mod.socket.SocketedGem;

import java.util.*;

public final class GemAttributeApplier {
    private GemAttributeApplier() {}

    private static final String MOD_ID = "ggd_gems_mod";
    private static final String MODIFIER_PATH_PREFIX = "gem/";

    // Разрешённые атрибуты (то, что ты хочешь использовать)
    private static final Set<Identifier> ALLOWED = Set.of(
            Identifier.of("minecraft", "attack_damage"),
            Identifier.of("minecraft", "attack_speed"),
            Identifier.of("minecraft", "attack_knockback"),
            Identifier.of("minecraft", "sweeping_damage_ratio"),
            Identifier.of("minecraft", "armor"),
            Identifier.of("minecraft", "knockback_resistance"),
            Identifier.of("minecraft", "explosion_knockback_resistance"),

            Identifier.of("minecraft", "max_health"),
            Identifier.of("minecraft", "max_absorption"),
            Identifier.of("minecraft", "luck"),
            Identifier.of("minecraft", "oxygen_bonus"),
            Identifier.of("minecraft", "fall_damage_multiplier"),

            Identifier.of("minecraft", "movement_speed"),
            Identifier.of("minecraft", "jump_strength"),

            Identifier.of("minecraft", "block_break_speed"),
            Identifier.of("minecraft", "mining_efficiency"),
            Identifier.of("minecraft", "submerged_mining_speed")
    );

    /**
     * Пересобирает ATTRIBUTE_MODIFIERS на предмете:
     * - сохраняет ванильные/чужие модификаторы
     * - удаляет только наши (ggd_gems_mod:gem/...)
     * - добавляет новые по socketed_gems
     */
    public static void rebuild(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        GemsConfig cfg = GemsConfigManager.get();
        if (cfg == null || cfg.byType == null) return;

        // 1) Суммируем бонусы по атрибуту
        Map<Identifier, Double> totals = new HashMap<>();

        List<SocketedGem> gems = GemSocketing.getSocketed(stack);
        for (SocketedGem g : gems) {
            if (g == null) continue;

            GemsConfig.GemDef def = cfg.byType.get(g.type());
            if (def == null || def.stat == null || def.stat.isBlank()) continue;

            Identifier attrId = parseStatId(def.stat);
            if (attrId == null) continue;

            // allowlist
            if (!ALLOWED.contains(attrId)) continue;

            int level = Math.max(1, g.level());
            int max = (cfg.maxLevel > 0) ? cfg.maxLevel : 10;
            level = Math.min(level, max);

            double total = def.base + def.perLevel * Math.max(0, level - 1);
            if (total == 0.0) continue;

            totals.merge(attrId, total, Double::sum);
        }

        AttributeModifierSlot slot = guessSlot(stack);

        // 2) Берём существующие модификаторы и удаляем только наши
        AttributeModifiersComponent existing = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();

        if (existing != null) {
            for (AttributeModifiersComponent.Entry e : existing.modifiers()) {
                Identifier id = e.modifier().id();
                if (!isOurModifierId(id)) {
                    builder.add(e.attribute(), e.modifier(), e.slot(), e.display());
                }
            }
        }

        // 3) Добавляем наши модификаторы
        for (Map.Entry<Identifier, Double> entry : totals.entrySet()) {
            Identifier attrId = entry.getKey();
            double value = entry.getValue();

            RegistryEntry<EntityAttribute> attr = attributeEntryFor(attrId);
            if (attr == null) continue;

            Identifier modId = modifierId(attrId, slot);

            EntityAttributeModifier modifier = new EntityAttributeModifier(
                    modId,
                    value,
                    EntityAttributeModifier.Operation.ADD_VALUE
            );

            builder.add(attr, modifier, slot);
        }

        // 4) Сохраняем
        stack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
    }

    private static Identifier parseStatId(String stat) {
        String s = stat.trim();

        // поддержка "minecraft:attack_damage"
        Identifier id = Identifier.tryParse(s);
        if (id != null) return id;

        // поддержка старого формата "ATTACK_DAMAGE"
        String u = s.toUpperCase(Locale.ROOT);
        return switch (u) {
            case "ATTACK_DAMAGE" -> Identifier.of("minecraft", "attack_damage");
            case "ATTACK_SPEED" -> Identifier.of("minecraft", "attack_speed");
            case "ATTACK_KNOCKBACK" -> Identifier.of("minecraft", "attack_knockback");
            case "SWEEPING_DAMAGE_RATIO" -> Identifier.of("minecraft", "sweeping_damage_ratio");
            case "ARMOR" -> Identifier.of("minecraft", "armor");
            case "KNOCKBACK_RESISTANCE" -> Identifier.of("minecraft", "knockback_resistance");
            case "EXPLOSION_KNOCKBACK_RESISTANCE" -> Identifier.of("minecraft", "explosion_knockback_resistance");
            case "MAX_HEALTH" -> Identifier.of("minecraft", "max_health");
            case "MAX_ABSORPTION" -> Identifier.of("minecraft", "max_absorption");
            case "LUCK" -> Identifier.of("minecraft", "luck");
            case "OXYGEN_BONUS" -> Identifier.of("minecraft", "oxygen_bonus");
            case "FALL_DAMAGE_MULTIPLIER" -> Identifier.of("minecraft", "fall_damage_multiplier");
            case "MOVEMENT_SPEED" -> Identifier.of("minecraft", "movement_speed");
            case "JUMP_STRENGTH" -> Identifier.of("minecraft", "jump_strength");
            case "BLOCK_BREAK_SPEED" -> Identifier.of("minecraft", "block_break_speed");
            case "MINING_EFFICIENCY" -> Identifier.of("minecraft", "mining_efficiency");
            case "SUBMERGED_MINING_SPEED" -> Identifier.of("minecraft", "submerged_mining_speed");
            default -> null;
        };
    }

    private static boolean isOurModifierId(Identifier id) {
        return id != null
                && MOD_ID.equals(id.getNamespace())
                && id.getPath() != null
                && id.getPath().startsWith(MODIFIER_PATH_PREFIX);
    }

    private static Identifier modifierId(Identifier attrId, AttributeModifierSlot slot) {
        // ggd_gems_mod:gem/minecraft/attack_damage/mainhand
        String path = MODIFIER_PATH_PREFIX
                + attrId.getNamespace() + "/"
                + attrId.getPath() + "/"
                + slot.asString();
        return Identifier.of(MOD_ID, path);
    }

    private static AttributeModifierSlot guessSlot(ItemStack stack) {
        EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
        if (eq != null) {
            EquipmentSlot s = eq.slot();
            return AttributeModifierSlot.forEquipmentSlot(s);
        }
        return AttributeModifierSlot.MAINHAND;
    }

    private static RegistryEntry<EntityAttribute> attributeEntryFor(Identifier id) {
        EntityAttribute attr = Registries.ATTRIBUTE.get(id);
        if (attr == null) return null;
        return Registries.ATTRIBUTE.getEntry(attr);
    }
}
