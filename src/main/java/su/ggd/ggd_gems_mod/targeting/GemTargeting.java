package su.ggd.ggd_gems_mod.targeting;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.registry.ModItems;
import net.minecraft.registry.tag.ItemTags;


public final class GemTargeting {
    private GemTargeting() {}

    public static boolean canInsertGemInto(ItemStack target, GemsConfig.GemDef def) {
        if (def == null) return false;
        if (target == null || target.isEmpty()) return false;
        if (def.targets == null || def.targets.isEmpty()) return false;

        // нельзя вковывать в сам самоцвет
        if (target.isOf(ModItems.GEM)) return false;

        for (String t : def.targets) {
            if (matchesTarget(target, t)) return true;
        }
        return false;
    }

    private static boolean matchesTarget(ItemStack stack, String target) {
        if (target == null) return false;
        String t = target.trim().toUpperCase();

        // ARMOR slots через EquippableComponent
        EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
        if (eq != null) {
            EquipmentSlot slot = eq.slot();

            return switch (t) {
                case "BOOTS" -> slot == EquipmentSlot.FEET;
                case "HELMET" -> slot == EquipmentSlot.HEAD;
                case "CHESTPLATE" -> slot == EquipmentSlot.CHEST;
                case "LEGGINGS" -> slot == EquipmentSlot.LEGS;
                case "ARMOR" -> slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                        || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
                default -> false;
            };
        }

        // WEAPON
        if (t.equals("WEAPON")) {
            return stack.isIn(ItemTags.SWORDS)
                    || stack.isIn(ItemTags.AXES)
                    || stack.isOf(Items.BOW)
                    || stack.isOf(Items.CROSSBOW);
        }

        // TOOLS (инструменты)
        if (t.equals("TOOLS") || t.equals("TOOL")) {
            return stack.isIn(ItemTags.PICKAXES)
                    || stack.isIn(ItemTags.SHOVELS)
                    || stack.isIn(ItemTags.AXES)
                    || stack.isIn(ItemTags.HOES)
                    || stack.isOf(Items.SHEARS);
        }

        return false;
    }
}
