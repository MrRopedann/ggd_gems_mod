package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.OnCraftRefundChance;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public final class LiterateCreatorEffect implements PassiveSkillEffect, OnCraftRefundChance {

    private static final double DEFAULT_CHANCE_PER_LEVEL = 0.015; // 1.5%

    @Override
    public String effectId() {
        return "literate_creator";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.maxLevel <= 0) { def.maxLevel = 10; changed = true; }
        if (def.costBase < 0) { def.costBase = 0; changed = true; }
        if (def.costPerLevel < 0) { def.costPerLevel = 0; changed = true; }

        if (def.params != null && !def.params.containsKey("chancePerLevel")) {
            def.params.put("chancePerLevel", DEFAULT_CHANCE_PER_LEVEL);
            changed = true;
        }
        return changed;
    }

    @Override
    public double getRefundChance(ServerWorld world, ServerPlayerEntity player, ItemStack crafted,
                                  PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (level <= 0) return 0.0;
        if (crafted == null || crafted.isEmpty()) return 0.0;
        if (!isToolWeaponArmor(crafted)) return 0.0;

        double perLv = JsonParamUtil.getDouble(def.params, "chancePerLevel", DEFAULT_CHANCE_PER_LEVEL);
        double chance = perLv * (double) level;
        if (chance < 0.0) chance = 0.0;
        if (chance > 1.0) chance = 1.0;
        return chance;
    }

    private static boolean isToolWeaponArmor(ItemStack s) {
        // инструменты
        if (s.isIn(net.minecraft.registry.tag.ItemTags.PICKAXES)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.AXES)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.HOES)) return true;

        // оружие
        if (s.isIn(net.minecraft.registry.tag.ItemTags.SWORDS)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.SPEARS)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.BOW_ENCHANTABLE)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.CROSSBOW_ENCHANTABLE)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.TRIDENT_ENCHANTABLE)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.WEAPON_ENCHANTABLE)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.MACE_ENCHANTABLE)) return true;

        // броня (любая)
        if (s.isIn(net.minecraft.registry.tag.ItemTags.ARMOR_ENCHANTABLE)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.HEAD_ARMOR)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.CHEST_ARMOR)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.LEG_ARMOR)) return true;
        if (s.isIn(net.minecraft.registry.tag.ItemTags.FOOT_ARMOR)) return true;

        // щит (если у тебя он должен попадать под "броня/экипировка")
        if (s.isIn(net.minecraft.registry.tag.ItemTags.EQUIPPABLE_ENCHANTABLE)) return true;

        return false;
    }

}
