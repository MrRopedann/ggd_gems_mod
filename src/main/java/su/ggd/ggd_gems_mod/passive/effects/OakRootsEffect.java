package su.ggd.ggd_gems_mod.passive.effects;

import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import su.ggd.ggd_gems_mod.passive.api.*;

import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class OakRootsEffect implements PassiveSkillEffect, OnBreakSpeed, OnBlockBreakAfter {

    @Override public String effectId() { return "oak_roots"; }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }

        JsonParamUtil.putIfMissing(def.params, "speedPercentPerLevel", 0.05);
        JsonParamUtil.putIfMissing(def.params, "speedCapLevel", 9);

        JsonParamUtil.putIfMissing(def.params, "treeFellerChancePerLevel", 0.01);
        JsonParamUtil.putIfMissing(def.params, "treeFellerLevel", 10);

        JsonParamUtil.putIfMissing(def.params, "breakLeaves", true);
        JsonParamUtil.putIfMissing(def.params, "radius", 8);
        JsonParamUtil.putIfMissing(def.params, "maxBlocks", 256);
        return false;
    }

    @Override
    public float modifyBreakSpeed(PlayerEntity player, BlockState state, ItemStack tool, float currentSpeed,
                                  PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (state == null || tool == null) return currentSpeed;
        if (!state.isIn(BlockTags.LOGS)) return currentSpeed;
        if (!tool.isIn(ItemTags.AXES)) return currentSpeed;

        int cap = JsonParamUtil.getInt(def.params, "speedCapLevel", 9);
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
        if (state == null || !state.isIn(BlockTags.LOGS)) return;

        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty() || !tool.isIn(ItemTags.AXES)) return;

        double chancePerLevel = JsonParamUtil.getDouble(def.params, "treeFellerChancePerLevel", 0.01);
        if (chancePerLevel <= 0) return;

        double chance = Math.min(1.0, level * chancePerLevel);
        if (world.random.nextDouble() >= chance) return;

        boolean breakLeaves = JsonParamUtil.getBool(def.params, "breakLeaves", true);
        int radius = Math.max(1, JsonParamUtil.getInt(def.params, "radius", 8));
        int maxBlocks = Math.max(1, JsonParamUtil.getInt(def.params, "maxBlocks", 256));

        Set<BlockPos> logs = collectConnectedLogs(world, pos, radius, maxBlocks);

        for (BlockPos p : logs) {
            if (p.equals(pos)) continue;
            BlockState s = world.getBlockState(p);
            if (s.isAir()) continue;
            if (!s.isIn(BlockTags.LOGS)) continue;
            world.breakBlock(p, true, player);
        }

        if (breakLeaves) {
            int leafRadius = 3;
            int maxLeaves = Math.max(256, Math.min(4000, maxBlocks * 12));
            Set<BlockPos> leaves = collectLeavesAroundLogs(world, logs, leafRadius, maxLeaves);

            for (BlockPos p : leaves) {
                BlockState s = world.getBlockState(p);
                if (s.isAir()) continue;
                if (!s.isIn(BlockTags.LEAVES)) continue;
                world.breakBlock(p, true, player);
            }
        }
    }

    private static Set<BlockPos> collectConnectedLogs(ServerWorld world, BlockPos origin, int radius, int maxBlocks) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        visited.add(origin);
        q.add(origin);

        while (!q.isEmpty() && visited.size() < maxBlocks) {
            BlockPos p = q.poll();
            for (Vec3i d : NEIGHBORS_26) {
                BlockPos np = p.add(d);
                if (!visited.add(np)) continue;
                if (!withinRadius(origin, np, radius)) continue;

                BlockState st = world.getBlockState(np);
                if (st != null && st.isIn(BlockTags.LOGS)) q.add(np);

                if (visited.size() >= maxBlocks) break;
            }
        }
        return visited;
    }

    private static Set<BlockPos> collectLeavesAroundLogs(ServerWorld world, Set<BlockPos> logs, int leafRadius, int maxLeaves) {
        Set<BlockPos> out = new HashSet<>();
        for (BlockPos log : logs) {
            for (int dx = -leafRadius; dx <= leafRadius; dx++) {
                for (int dy = -leafRadius; dy <= leafRadius; dy++) {
                    for (int dz = -leafRadius; dz <= leafRadius; dz++) {
                        if (out.size() >= maxLeaves) return out;
                        BlockPos p = log.add(dx, dy, dz);
                        BlockState s = world.getBlockState(p);
                        if (s != null && s.isIn(BlockTags.LEAVES)) out.add(p);
                    }
                }
            }
        }
        return out;
    }

    private static boolean withinRadius(BlockPos c, BlockPos p, int r) {
        int dx = Math.abs(p.getX() - c.getX());
        int dy = Math.abs(p.getY() - c.getY());
        int dz = Math.abs(p.getZ() - c.getZ());
        return dx <= r && dy <= r && dz <= r;
    }

    private static final Vec3i[] NEIGHBORS_26 = buildNeighbors26();
    private static Vec3i[] buildNeighbors26() {
        java.util.ArrayList<Vec3i> list = new java.util.ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    list.add(new Vec3i(dx, dy, dz));
                }
        return list.toArray(new Vec3i[0]);
    }
}
