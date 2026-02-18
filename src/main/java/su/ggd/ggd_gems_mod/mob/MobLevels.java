package su.ggd.ggd_gems_mod.mob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import su.ggd.ggd_gems_mod.npc.GemTraderNpc;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

import java.util.HashMap;
import java.util.Map;

public final class MobLevels {
    private MobLevels() {}

    private static boolean DONE = false;

    private static final int MAX_LEVEL = 20;
    private static final double DIST_PER_LEVEL = 180.0;
    private static final int EXTRA_XP_PER_LEVEL = 2;

    private static final String TAG_BASE_NORMALIZED = "ggd_base_norm";
    private static final String TAG_LEVEL_NAME = "ggd_lvl_name";

    private static final Identifier MOD_HEALTH = Identifier.of(Ggd_gems_mod.MOD_ID, "mob_level_health");
    private static final Identifier MOD_ARMOR  = Identifier.of(Ggd_gems_mod.MOD_ID, "mob_level_armor");
    private static final Identifier MOD_ATTACK = Identifier.of(Ggd_gems_mod.MOD_ID, "mob_level_attack");
    private static final Identifier MOD_SPEED  = Identifier.of(Ggd_gems_mod.MOD_ID, "mob_level_speed");

    private static final Map<Identifier, BaseStats> BASE = new HashMap<>();
    static {
        putBase("minecraft:zombie",     20.0, 3.0, 0.0, 0.23);
        putBase("minecraft:husk",       20.0, 3.0, 0.0, 0.23);
        putBase("minecraft:drowned",    20.0, 3.0, 0.0, 0.23);

        putBase("minecraft:skeleton",   20.0, 2.0, 0.0, 0.25);
        putBase("minecraft:stray",      20.0, 2.0, 0.0, 0.25);

        putBase("minecraft:spider",     16.0, 2.0, 0.0, 0.30);
        putBase("minecraft:cave_spider",12.0, 2.0, 0.0, 0.30);

        putBase("minecraft:creeper",    20.0, 0.0, 0.0, 0.25);
        putBase("minecraft:enderman",   40.0, 7.0, 0.0, 0.30);

        putBase("minecraft:witch",      26.0, 2.0, 0.0, 0.25);
    }

    private static void putBase(String id, double hp, double attack, double armor, double speed) {
        BASE.put(Identifier.of(id), new BaseStats(hp, attack, armor, speed));
    }

    public static void init() {
        if (DONE) return;
        DONE = true;

        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerWorld world) -> {
            if (world == null || entity == null) return;
            if (!(entity instanceof MobEntity mob)) return;

            if (isOurTraderNpc(entity)) return;
            if (isExcluded(mob)) return;

            normalizeBaseOnce(mob);
            ensureLevelAndApply(world, mob);
        });

        EntityTrackingEvents.START_TRACKING.register((Entity entity, ServerPlayerEntity player) -> {
            if (!(entity instanceof MobEntity mob)) return;

            if (isOurTraderNpc(entity)) return;
            if (isExcluded(mob)) return;

            if (!MobLevelUtil.hasLevelTag(mob)) {
                if (mob.getEntityWorld() instanceof ServerWorld world) {
                    normalizeBaseOnce(mob);
                    ensureLevelAndApply(world, mob);
                }
            }

            int lvl = MobLevelUtil.getLevel(mob);
            su.ggd.ggd_gems_mod.net.MobLevelSyncServer.sendTo(player, mob, lvl);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource src) -> {
            if (!(entity.getEntityWorld() instanceof ServerWorld world)) return;
            if (!(entity instanceof MobEntity mob)) return;

            if (isOurTraderNpc(entity)) return;
            if (isExcluded(mob)) return;

            int lvl = MobLevelUtil.getLevel(mob);
            int extra = Math.max(0, (lvl - 1) * EXTRA_XP_PER_LEVEL);
            if (extra <= 0) return;

            world.spawnEntity(new ExperienceOrbEntity(world, mob.getX(), mob.getY(), mob.getZ(), extra));
        });
    }

    // только жители исключены
    private static boolean isExcluded(MobEntity mob) {
        return mob instanceof VillagerEntity || mob instanceof WanderingTraderEntity;
    }

    private static boolean isOurTraderNpc(Entity e) {
        if (e == null) return false;
        for (String tag : e.getCommandTags()) {
            if (tag != null && tag.startsWith(GemTraderNpc.TAG_PREFIX)) return true;
        }
        return false;
    }

    private static void normalizeBaseOnce(MobEntity mob) {
        if (mob.getCommandTags().contains(TAG_BASE_NORMALIZED)) return;

        Identifier typeId = Registries.ENTITY_TYPE.getId(mob.getType());
        BaseStats s = BASE.get(typeId);
        if (s == null) return;

        removeModifier(mob, EntityAttributes.MAX_HEALTH, MOD_HEALTH);
        removeModifier(mob, EntityAttributes.ARMOR, MOD_ARMOR);
        removeModifier(mob, EntityAttributes.ATTACK_DAMAGE, MOD_ATTACK);
        removeModifier(mob, EntityAttributes.MOVEMENT_SPEED, MOD_SPEED);

        setBase(mob, EntityAttributes.MAX_HEALTH, s.maxHealth);
        setBase(mob, EntityAttributes.ATTACK_DAMAGE, s.attackDamage);
        setBase(mob, EntityAttributes.ARMOR, s.armor);
        setBase(mob, EntityAttributes.MOVEMENT_SPEED, s.moveSpeed);

        try { mob.setHealth(mob.getMaxHealth()); } catch (Throwable ignored) {}

        mob.addCommandTag(TAG_BASE_NORMALIZED);
    }

    private static void setBase(MobEntity mob, RegistryEntry<EntityAttribute> attr, double value) {
        EntityAttributeInstance inst = mob.getAttributeInstance(attr);
        if (inst == null) return;
        inst.setBaseValue(value);
    }

    private static void ensureLevelAndApply(ServerWorld world, MobEntity mob) {
        int lvl;
        if (!MobLevelUtil.hasLevelTag(mob)) {
            lvl = computeLevel(world, mob);
            MobLevelUtil.setLevel(mob, lvl);
        } else {
            lvl = MobLevelUtil.getLevel(mob);
        }

        applyLevelScaling(mob, lvl);

        clearGeneratedLevelName(mob);

        su.ggd.ggd_gems_mod.net.MobLevelSyncServer.send(mob, lvl);
    }

    private static void clearGeneratedLevelName(MobEntity mob) {
        if (mob.getCommandTags().contains(TAG_LEVEL_NAME)) {
            mob.setCustomName(null);
            mob.setCustomNameVisible(false);
            mob.removeCommandTag(TAG_LEVEL_NAME);
        }
    }

    private static int computeLevel(ServerWorld world, MobEntity mob) {
        BlockPos spawn = world.getLevelProperties().getSpawnPoint().getPos();

        double dx = mob.getX() - (spawn.getX() + 0.5);
        double dz = mob.getZ() - (spawn.getZ() + 0.5);
        double dist = Math.sqrt(dx * dx + dz * dz);

        int base = 1 + (int) Math.floor(dist / DIST_PER_LEVEL);
        int jitter = ((mob.getUuid().hashCode() & 1) == 0) ? 0 : 1;

        return MathHelper.clamp(base + jitter, 1, MAX_LEVEL);
    }

    private static void applyLevelScaling(MobEntity mob, int level) {
        removeModifier(mob, EntityAttributes.MAX_HEALTH, MOD_HEALTH);
        removeModifier(mob, EntityAttributes.ARMOR, MOD_ARMOR);
        removeModifier(mob, EntityAttributes.ATTACK_DAMAGE, MOD_ATTACK);
        removeModifier(mob, EntityAttributes.MOVEMENT_SPEED, MOD_SPEED);

        if (level <= 1) return;

        double healthMul = 0.15 * (level - 1);
        double attackMul = 0.12 * (level - 1);
        double speedMul  = 0.01 * (level - 1);
        double armorAdd  = 0.35 * (level - 1);

        addMulTotal(mob, EntityAttributes.MAX_HEALTH, MOD_HEALTH, healthMul);
        addMulTotal(mob, EntityAttributes.ATTACK_DAMAGE, MOD_ATTACK, attackMul);
        addMulTotal(mob, EntityAttributes.MOVEMENT_SPEED, MOD_SPEED, speedMul);
        addAddValue(mob, EntityAttributes.ARMOR, MOD_ARMOR, armorAdd);

        try { mob.setHealth(mob.getMaxHealth()); } catch (Throwable ignored) {}
    }

    private static void removeModifier(MobEntity mob, RegistryEntry<EntityAttribute> attr, Identifier id) {
        EntityAttributeInstance inst = mob.getAttributeInstance(attr);
        if (inst == null) return;
        inst.removeModifier(id);
    }

    private static void addMulTotal(MobEntity mob, RegistryEntry<EntityAttribute> attr, Identifier id, double amount) {
        if (amount == 0.0) return;
        EntityAttributeInstance inst = mob.getAttributeInstance(attr);
        if (inst == null) return;

        inst.addPersistentModifier(new EntityAttributeModifier(
                id, amount, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void addAddValue(MobEntity mob, RegistryEntry<EntityAttribute> attr, Identifier id, double amount) {
        if (amount == 0.0) return;
        EntityAttributeInstance inst = mob.getAttributeInstance(attr);
        if (inst == null) return;

        inst.addPersistentModifier(new EntityAttributeModifier(
                id, amount, EntityAttributeModifier.Operation.ADD_VALUE
        ));
    }

    private record BaseStats(double maxHealth, double attackDamage, double armor, double moveSpeed) {}
}
