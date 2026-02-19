package su.ggd.ggd_gems_mod.npc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.Heightmap;
import su.ggd.ggd_gems_mod.config.NpcTradersConfig;
import su.ggd.ggd_gems_mod.config.NpcTradersConfigManager;

import java.util.Locale;
import java.util.function.Predicate;

public final class GemTraderNpc {

    // общий префикс, чтобы отличать наших NPC
    public static final String TAG_PREFIX = "ggd_trader:";

    private GemTraderNpc() {}

    public static Entity spawnForPlayer(ServerPlayerEntity player, String traderId) {
        ServerWorld world = player.getEntityWorld();
        if (world == null) return null;

        NpcTradersConfig cfg = NpcTradersConfigManager.get();
        if (cfg == null) return null;

        String id = (traderId == null ? "" : traderId.trim().toLowerCase(Locale.ROOT));
        if (id.isBlank()) id = "gems";

        NpcTradersConfig.TraderDef def = cfg.byId.get(id);
        if (def == null) return null;

        // позиция на поверхности перед игроком
        Direction dir = player.getHorizontalFacing();
        int dist = Math.max(1, (int) Math.round(def.spawnDistance));

        BlockPos target = player.getBlockPos().offset(dir, dist);
        BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, target);

        Vec3d pos = new Vec3d(top.getX() + 0.5, top.getY() + 0.05, top.getZ() + 0.5);

        // тип сущности
        Identifier typeId = Identifier.tryParse(def.entityType);
        EntityType<?> entityType = (typeId != null) ? Registries.ENTITY_TYPE.get(typeId) : EntityType.VILLAGER;

        Entity entity = entityType.create(world, SpawnReason.COMMAND);
        if (entity == null) return null;

        // повернуть к игроку
        float[] rot = lookAt(pos, new Vec3d(player.getX(), player.getEyeY(), player.getZ()));
        entity.refreshPositionAndAngles(pos.x, pos.y, pos.z, rot[0], rot[1]);

        entity.addCommandTag(TAG_PREFIX + id);

        // отображаем только нормальное имя
        entity.setCustomName(Text.literal(def.name));
        entity.setCustomNameVisible(def.nameVisible);

        if (entity instanceof MobEntity mob) {
            mob.setAiDisabled(def.noAi);
            mob.setPersistent();
        }

        // Ванильные трейды не используем: у нас кастомное меню торговли.
        if (entity instanceof VillagerEntity villager) {
            applyVillagerProfession(villager, def.profession);
            villager.getOffers().clear();
        } else if (entity instanceof net.minecraft.entity.passive.WanderingTraderEntity trader) {
            trader.getOffers().clear();
        }

        world.spawnEntity(entity);
        return entity;
    }

    public static Entity getLookedAtTrader(ServerPlayerEntity player, double distance) {
        ServerWorld world = player.getEntityWorld();
        if (world == null) return null;

        Vec3d start = new Vec3d(player.getX(), player.getEyeY(), player.getZ());
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(distance));

        Box box = player.getBoundingBox()
                .stretch(player.getRotationVec(1.0f).multiply(distance))
                .expand(1.0);

        Predicate<Entity> filter = e -> e != null && e.isAlive() && isOurTrader(e);

        EntityHitResult hit = ProjectileUtil.raycast(
                player, start, end, box, filter, distance * distance
        );

        return hit != null ? hit.getEntity() : null;
    }

    public static boolean hasOurTraderTag(Entity e) {
        for (String tag : e.getCommandTags()) {
            if (tag != null && tag.startsWith(TAG_PREFIX)) return true;
        }
        return false;
    }

    private static float[] lookAt(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double horiz = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));

        return new float[] { yaw, pitch };
    }

    private static void applyVillagerProfession(VillagerEntity villager, String professionId) {
        Identifier id = Identifier.tryParse(professionId);
        if (id == null) return;

        RegistryEntry<VillagerProfession> entry =
                Registries.VILLAGER_PROFESSION.getEntry(id).orElse(null);
        if (entry == null) return;

        VillagerData data = villager.getVillagerData();
        villager.setVillagerData(data.withProfession(entry));
    }

    public static boolean isOurTrader(Entity e) {
        if (e == null) return false;
        for (String tag : e.getCommandTags()) {
            if (tag != null && tag.startsWith(TAG_PREFIX)) return true;
        }
        return false;
    }
}
