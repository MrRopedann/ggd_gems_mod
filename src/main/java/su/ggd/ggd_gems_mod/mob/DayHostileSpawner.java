package su.ggd.ggd_gems_mod.mob;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import su.ggd.ggd_gems_mod.config.MobRulesConfig;
import su.ggd.ggd_gems_mod.config.MobRulesConfigManager;

import java.util.Map;
import java.util.WeakHashMap;

public final class DayHostileSpawner {
    private DayHostileSpawner() {}

    private static final int MIN_DIST = 24;
    private static final int MAX_DIST = 128;

    private static final Map<RegistryKey<World>, Integer> WORLD_TICKS = new WeakHashMap<>();

    private static boolean DONE = false;

    public static void init() {
        if (DONE) return;
        DONE = true;

        ServerTickEvents.END_WORLD_TICK.register(DayHostileSpawner::onWorldTick);
    }

    private static void onWorldTick(ServerWorld world) {
        MobRulesConfig cfg = MobRulesConfigManager.get();
        if (cfg == null) return;

        if (!cfg.hostilesSpawnDaytime) return;
        if (world.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!world.isDay()) return;
        if (world.getPlayers().isEmpty()) return;

        int interval = Math.max(1, cfg.daytimeSpawnIntervalTicks);
        int t = WORLD_TICKS.getOrDefault(world.getRegistryKey(), 0) + 1;
        WORLD_TICKS.put(world.getRegistryKey(), t);
        if ((t % interval) != 0) return;

        int cap = computeMonsterCap(cfg);
        int current = countHostiles(world);
        if (current >= cap) return;

        int attempts = MathHelper.clamp(cfg.daytimeSpawnAttempts, 1, 64);
        for (int i = 0; i < attempts; i++) {
            if (current >= cap) break;
            if (trySpawnOnce(world, cfg)) current++;
        }
    }

    private static int computeMonsterCap(MobRulesConfig cfg) {
        if (cfg.monsterCapOverride > 0) return cfg.monsterCapOverride;

        int base = 70;
        double mul = cfg.monsterCapMultiplier;
        if (!Double.isFinite(mul) || mul <= 0.0) mul = 1.0;
        return Math.max(1, (int) Math.round(base * mul));
    }

    private static int countHostiles(ServerWorld world) {
        int c = 0;
        for (var e : world.iterateEntities()) {
            if (e instanceof HostileEntity) c++;
        }
        return c;
    }

    private static boolean trySpawnOnce(ServerWorld world, MobRulesConfig cfg) {
        ServerPlayerEntity anchor = world.getPlayers().get(world.getRandom().nextInt(world.getPlayers().size()));
        Vec3d a = new Vec3d(anchor.getX(), anchor.getY(), anchor.getZ());

        double angle = world.getRandom().nextDouble() * (Math.PI * 2.0);
        double dist = MIN_DIST + world.getRandom().nextDouble() * (MAX_DIST - MIN_DIST);
        int x = MathHelper.floor(a.x + Math.cos(angle) * dist);
        int z = MathHelper.floor(a.z + Math.sin(angle) * dist);

        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (topY <= world.getBottomY() + 2) return false;

        BlockPos ground = new BlockPos(x, topY - 1, z);
        BlockPos spawnPos = ground.up();

        double nearest = nearestPlayerDistance(world, spawnPos);
        if (nearest < MIN_DIST || nearest > MAX_DIST) return false;

        if (!isValidGround(world, ground)) return false;

        if (!world.getBlockState(spawnPos).getCollisionShape(world, spawnPos).isEmpty()) return false;
        if (!world.getBlockState(spawnPos.up()).getCollisionShape(world, spawnPos.up()).isEmpty()) return false;

        if (!cfg.daytimeIgnoreLight) {
            int light = world.getLightLevel(spawnPos);
            int max = MathHelper.clamp(cfg.daytimeMaxLight, 0, 15);
            if (light > max) return false;
        }

        EntityType<? extends MobEntity> type = pickHostileType(world, spawnPos);
        Entity e = type.create(world, cfg.daytimeIgnoreLight ? SpawnReason.EVENT : SpawnReason.NATURAL);
        if (!(e instanceof MobEntity mob)) return false;

        mob.refreshPositionAndAngles(
                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                world.getRandom().nextFloat() * 360.0f, 0.0f
        );

        if (!cfg.daytimeIgnoreLight) {
            if (!SpawnRestriction.canSpawn(type, world, SpawnReason.NATURAL, spawnPos, world.getRandom())) return false;
        } else {
            if (!customCanSpawnOnSurface(world, spawnPos)) return false;
        }

        mob.initialize(world, world.getLocalDifficulty(spawnPos),
                cfg.daytimeIgnoreLight ? SpawnReason.EVENT : SpawnReason.NATURAL,
                null);

        return world.spawnEntity(mob);
    }

    private static double nearestPlayerDistance(ServerWorld world, BlockPos pos) {
        double best = Double.POSITIVE_INFINITY;
        for (ServerPlayerEntity p : world.getPlayers()) {
            double d = p.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (d < best) best = d;
        }
        return Math.sqrt(best);
    }

    private static boolean customCanSpawnOnSurface(ServerWorld world, BlockPos spawnPos) {
        if (!world.getFluidState(spawnPos).isEmpty()) return false;
        if (!world.getFluidState(spawnPos.up()).isEmpty()) return false;
        if (!world.getBlockState(spawnPos).isAir()) return false;
        if (!world.getBlockState(spawnPos.up()).isAir()) return false;
        return true;
    }

    private static boolean isValidGround(ServerWorld world, BlockPos ground) {
        BlockState st = world.getBlockState(ground);

        if (!st.isOpaqueFullCube()) return false;

        if (st.getBlock() instanceof LeavesBlock) return false;
        if (st.getBlock() instanceof CarpetBlock) return false;
        if (st.isIn(BlockTags.LEAVES)) return false;
        if (st.isIn(BlockTags.ICE)) return false;
        if (st.isIn(BlockTags.SLABS)) return false;
        if (st.isIn(BlockTags.STAIRS)) return false;

        String cls = st.getBlock().getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        if (cls.contains("glass")) return false;

        return true;
    }

    private static EntityType<? extends MobEntity> pickHostileType(ServerWorld world, BlockPos pos) {
        String biomeId = world.getBiome(pos).getKey().map(k -> k.getValue().toString()).orElse("");
        String b = biomeId.toLowerCase(java.util.Locale.ROOT);

        if (b.contains("desert") || b.contains("badlands") || b.contains("mesa")) return EntityType.HUSK;
        if (b.contains("snow") || b.contains("ice") || b.contains("frozen") || b.contains("taiga") || b.contains("grove")) return EntityType.STRAY;

        int r = world.getRandom().nextInt(100);
        if (r < 45) return EntityType.ZOMBIE;
        if (r < 75) return EntityType.SKELETON;
        if (r < 90) return EntityType.SPIDER;
        return EntityType.CREEPER;
    }
}
