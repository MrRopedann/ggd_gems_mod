package su.ggd.ggd_gems_mod.mob;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarpetBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
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

        // --- Свет: либо ограничение (старый режим), либо игнор + взвешенный шанс ---
        int light = world.getLightLevel(spawnPos); // 0..15

        if (!cfg.daytimeIgnoreLight) {
            int max = MathHelper.clamp(cfg.daytimeMaxLight, 0, 15);
            if (light > max) return false;
        }

        if (cfg.daytimeLightWeightedChance) {
            double chance = computeChanceByLight(cfg, light);
            if (world.getRandom().nextDouble() > chance) return false;
        }

        EntityType<? extends MobEntity> type = pickHostileType(world, spawnPos);

        // EVENT: не упираемся в NATURAL ограничения, включая свет
        Entity e = type.create(world, SpawnReason.EVENT);
        if (!(e instanceof MobEntity mob)) return false;

        mob.refreshPositionAndAngles(
                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                world.getRandom().nextFloat() * 360.0f, 0.0f
        );

        // Кастомная проверка места (без vanilla SpawnRestriction)
        if (!customCanSpawnOnSurface(world, spawnPos)) return false;

        mob.initialize(world, world.getLocalDifficulty(spawnPos), SpawnReason.EVENT, null);

        return world.spawnEntity(mob);
    }

    private static double computeChanceByLight(MobRulesConfig cfg, int light0to15) {
        int l = MathHelper.clamp(light0to15, 0, 15);

        double min = cfg.daytimeChanceMinAtLight15;
        double max = cfg.daytimeChanceMaxAtLight0;

        if (!Double.isFinite(min)) min = 0.25;
        if (!Double.isFinite(max)) max = 1.0;

        min = clamp01(min);
        max = clamp01(max);

        // если кто-то выставил min > max — нормализуем
        if (min > max) {
            double tmp = min;
            min = max;
            max = tmp;
        }

        double power = cfg.daytimeChanceCurvePower;
        if (!Double.isFinite(power) || power < 0.1) power = 1.0;
        if (power > 10.0) power = 10.0;

        // t=0 (темно) -> 0, t=1 (светло) -> 1
        double t = l / 15.0;
        double shaped = Math.pow(t, power);

        // t=0 => chance=max, t=1 => chance=min
        double chance = max - (max - min) * shaped;
        return clamp01(chance);
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
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