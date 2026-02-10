package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.OnBlockBreakAfter;
import su.ggd.ggd_gems_mod.passive.api.OnBreakSpeed;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class LuckyMinerEffect implements PassiveSkillEffect, OnBreakSpeed, OnBlockBreakAfter {
    private static final String RUNTIME_ORE_TAGS = "oreTagsCompiled";

    @Override public String effectId() { return "lucky_miner"; }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }
        if (def.runtime == null) {
            def.runtime = new HashMap<>();
            changed = true;
        }

        JsonParamUtil.putIfMissing(def.params, "speedPercentPerLevel", 0.05);
        JsonParamUtil.putIfMissing(def.params, "doubleDropChancePerLevel", 0.01);
        JsonParamUtil.putIfMissing(def.params, "speedCapLevel", 10);

        // default ore tags (как List<String>, без JsonArray)
        if (!def.params.containsKey("oreBlockTags")) {
            List<String> tags = new ArrayList<>();
            tags.add("minecraft:coal_ores");
            tags.add("minecraft:iron_ores");
            tags.add("minecraft:gold_ores");
            tags.add("minecraft:copper_ores");
            tags.add("minecraft:diamond_ores");
            tags.add("minecraft:emerald_ores");
            tags.add("minecraft:lapis_ores");
            tags.add("minecraft:redstone_ores");
            def.params.put("oreBlockTags", tags);
            changed = true;
        }

        // compile runtime tags
        List<TagKey<net.minecraft.block.Block>> compiled = new ArrayList<>();
        for (String raw : JsonParamUtil.getStringList(def.params, "oreBlockTags", List.of())) {
            if (raw == null || raw.isBlank()) continue;
            try {
                Identifier id = Identifier.of(raw.trim());
                compiled.add(TagKey.of(RegistryKeys.BLOCK, id));
            } catch (Exception ignored) {}
        }
        def.runtime.put(RUNTIME_ORE_TAGS, compiled);

        return changed;
    }

    @Override
    public float modifyBreakSpeed(PlayerEntity player, BlockState state, ItemStack tool, float currentSpeed,
                                  PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (state == null || tool == null || def == null) return currentSpeed;
        if (!tool.isIn(ItemTags.PICKAXES)) return currentSpeed;
        if (!isOre(def, state)) return currentSpeed;

        int cap = JsonParamUtil.getInt(def.params, "speedCapLevel", 10);
        int capped = Math.min(level, cap);
        if (capped <= 0) return currentSpeed;

        double perLevel = JsonParamUtil.getDouble(def.params, "speedPercentPerLevel", 0.05);
        double bonus = perLevel * capped;
        if (bonus <= 0) return currentSpeed;

        float mul = (float) (1.0 + bonus);
        if (mul < 1.0f) mul = 1.0f;
        if (mul > 10.0f) mul = 10.0f;

        return currentSpeed * mul;
    }

    @Override
    public void onBlockBreakAfter(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state, BlockEntity be,
                                  PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (world == null || player == null || state == null || def == null) return;
        if (!isOre(def, state)) return;

        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty() || !tool.isIn(ItemTags.PICKAXES)) return;

        double perLevel = JsonParamUtil.getDouble(def.params, "doubleDropChancePerLevel", 0.01);
        if (perLevel <= 0) return;

        double chance = Math.min(1.0, level * perLevel);
        if (world.random.nextDouble() >= chance) return;

        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, be, player, tool);
        for (ItemStack s : drops) {
            if (s == null || s.isEmpty()) continue;
            Block.dropStack(world, pos, s);
        }
    }

    private static boolean isOre(PassiveSkillsConfig.PassiveSkillDef def, BlockState state) {
        if (def.runtime == null) return false;

        Object o = def.runtime.get(RUNTIME_ORE_TAGS);
        if (!(o instanceof List<?> list)) return false;

        for (Object t : list) {
            if (t instanceof TagKey<?> tag) {
                @SuppressWarnings("unchecked")
                TagKey<net.minecraft.block.Block> bt = (TagKey<net.minecraft.block.Block>) tag;
                if (state.isIn(bt)) return true;
            }
        }
        return false;
    }
}
