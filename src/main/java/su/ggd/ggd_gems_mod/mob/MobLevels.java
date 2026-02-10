package su.ggd.ggd_gems_mod.mob;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import su.ggd.ggd_gems_mod.config.NpcTradersConfigManager;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

import java.util.HashMap;
import java.util.Map;

public final class MobLevels {
    private MobLevels() {}

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
        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerWorld world) -> {
            if (world == null || entity == null) return;
            if (!(entity instanceof MobEntity mob)) return;
            if (isOurTraderNpc(mob)) return;

            normalizeBaseOnce(mob);
            ensureLevelAndApply(world, mob);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource src) -> {
            if (!(entity.getEntityWorld() instanceof ServerWorld world)) return;
            if (!(entity instanceof MobEntity mob)) return;
            if (isOurTraderNpc(mob)) return;

            int lvl = MobLevelUtil.getLevel(mob);
            int extra = Math.max(0, (lvl - 1) * EXTRA_XP_PER_LEVEL);
            if (extra <= 0) return;

            world.spawnEntity(new ExperienceOrbEntity(world, mob.getX(), mob.getY(), mob.getZ(), extra));
        });
    }

    private static void normalizeBaseOnce(MobEntity mob) {
        if (mob.getCommandTags().contains(TAG_BASE_NORMALIZED)) return;

        Identifier typeId = Registries.ENTITY_TYPE.getId(mob.getType());
        BaseStats s = BASE.get(typeId);
        if (s == null) return;

        // убрать наши модификаторы уровня перед нормализацией
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

        // 1) применяем скейлинг
        applyLevelScaling(mob, lvl);

        // 2) и после скейлинга — обновляем имя (чтобы показывало актуальные статы)
        applyLevelNameWithStats(mob, lvl);
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

        // HP — множитель ок
        double healthMul = 0.15 * (level - 1);

        // ATTACK — множитель ок (у большинства мобов база > 0)
        double attackMul = 0.12 * (level - 1);

        // SPEED — множитель ок
        double speedMul  = 0.01 * (level - 1);

        // ARMOR: у большинства мобов база 0, поэтому множитель бессмысленен.
        // Делаем плоский прирост брони.
        double armorAdd = 0.35 * (level - 1); // пример: Lv13 => +4.2 брони

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
                id,
                amount,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
    }

    private static void addAddValue(MobEntity mob, RegistryEntry<EntityAttribute> attr, Identifier id, double amount) {
        if (amount == 0.0) return;
        EntityAttributeInstance inst = mob.getAttributeInstance(attr);
        if (inst == null) return;

        inst.addPersistentModifier(new EntityAttributeModifier(
                id,
                amount,
                EntityAttributeModifier.Operation.ADD_VALUE
        ));
    }

    /**
     * Имя синхронизируется клиенту. Поэтому сюда же добавляем отображаемые статы,
     * а HUD просто выводит displayName без вычислений.
     */
    private static void applyLevelNameWithStats(MobEntity mob, int level) {
        // Если есть чужое кастомное имя, и оно не наше — не трогаем
        if (!mob.getCommandTags().contains(TAG_LEVEL_NAME) && mob.getCustomName() != null) return;

        Text baseName = mob.getType().getName();

        double armor = getAttrValue(mob, EntityAttributes.ARMOR);
        double atk   = getAttrValue(mob, EntityAttributes.ATTACK_DAMAGE);

        String armorS = trim2(armor);
        String atkS   = trim2(atk);

        // "[Lv 13] Скелет (🛡 4.2 ⚔ 4.9)"
        Text name = Text.literal("[Lv " + level + "] ")
                .append(baseName)
                .append(Text.literal(" (\uD83D\uDEE1 " + armorS + " \u2694 " + atkS + ")"));

        mob.setCustomName(name);
        mob.setCustomNameVisible(false);
        mob.addCommandTag(TAG_LEVEL_NAME);
    }

    private static double getAttrValue(LivingEntity e, RegistryEntry<EntityAttribute> attr) {
        EntityAttributeInstance inst = e.getAttributeInstance(attr);
        if (inst == null) return 0.0;
        return inst.getValue();
    }

    private static String trim2(double v) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static boolean isOurTraderNpc(MobEntity mob) {
        if (!(mob instanceof VillagerEntity) && !(mob instanceof WanderingTraderEntity)) return false;

        String display = mob.getDisplayName().getString();
        if (NpcTradersConfigManager.isNpcName(display)) return true;

        String custom = mob.getCustomName() != null ? mob.getCustomName().getString() : "";
        return custom.contains("ggd_trader:") || display.contains("ggd_trader:");
    }

    private record BaseStats(double maxHealth, double attackDamage, double armor, double moveSpeed) {}
}
