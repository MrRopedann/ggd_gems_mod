package su.ggd.ggd_gems_mod.npc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.Heightmap;

import net.minecraft.entity.projectile.ProjectileUtil;

import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.config.NpcTradersConfig;
import su.ggd.ggd_gems_mod.config.NpcTradersConfigManager;
import su.ggd.ggd_gems_mod.gem.GemStacks;
import su.ggd.ggd_gems_mod.registry.ModItems;

import java.util.*;
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

        // трейды
        if (entity instanceof VillagerEntity villager) {
            applyVillagerProfession(villager, def.profession);
            applyTrades(villager, def);
        } else if (entity instanceof net.minecraft.entity.passive.WanderingTraderEntity trader) {
            applyTradesToMerchant(trader.getOffers(), def);
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

    private static void applyTrades(VillagerEntity villager, NpcTradersConfig.TraderDef def) {
        TradeOfferList offers = villager.getOffers();
        offers.clear();
        applyTradesToMerchant(offers, def);
    }

    private static void applyTradesToMerchant(TradeOfferList offers, NpcTradersConfig.TraderDef def) {
        GemsConfig gemsCfg = GemsConfigManager.get();
        List<String> allGemTypes = new ArrayList<>();
        if (gemsCfg != null && gemsCfg.byType != null) allGemTypes.addAll(gemsCfg.byType.keySet());

        Random rnd = new Random();

        if (def.trades == null) return;

        for (NpcTradersConfig.TradeDef t : def.trades) {
            if (t == null || t.buy == null) continue;

            TradedItem buy = toTradedItem(t.buy);
            if (buy == null) continue;

            ItemStack sell = null;

            // sellGem имеет приоритет
            if (t.sellGem != null) {
                if (allGemTypes.isEmpty()) continue;

                String type = (t.sellGem.type == null ? "" : t.sellGem.type.trim().toLowerCase(Locale.ROOT));
                if (type.isBlank()) {
                    type = allGemTypes.get(rnd.nextInt(allGemTypes.size()));
                } else if (gemsCfg == null || gemsCfg.byType == null || !gemsCfg.byType.containsKey(type)) {
                    continue; // неизвестный gem type
                }

                sell = new ItemStack(ModItems.GEM);
                GemStacks.applyGemDataFromConfig(sell, type, Math.max(1, t.sellGem.level));
            } else if (t.sell != null) {
                sell = toItemStack(t.sell);
            }

            if (sell == null || sell.isEmpty()) continue;

            offers.add(new TradeOffer(
                    buy,
                    sell,
                    t.maxUses <= 0 ? 999999 : t.maxUses,
                    Math.max(0, t.merchantExperience),
                    t.priceMultiplier < 0 ? 0.0f : t.priceMultiplier
            ));
        }
    }

    private static TradedItem toTradedItem(NpcTradersConfig.StackDef def) {
        if (def == null || def.item == null || def.item.isBlank()) return null;

        Identifier id = Identifier.tryParse(def.item.trim());
        if (id == null) return null;

        Item item = Registries.ITEM.get(id);
        if (item == null) return null;

        int count = Math.max(1, def.count);
        return new TradedItem(item, count);
    }

    private static ItemStack toItemStack(NpcTradersConfig.StackDef def) {
        if (def == null || def.item == null || def.item.isBlank()) return ItemStack.EMPTY;

        Identifier id = Identifier.tryParse(def.item.trim());
        if (id == null) return ItemStack.EMPTY;

        Item item = Registries.ITEM.get(id);
        if (item == null) return ItemStack.EMPTY;

        int count = Math.max(1, def.count);
        return new ItemStack(item, count);
    }

    public static boolean isOurTrader(Entity e) {
        if (e == null) return false;
        for (String tag : e.getCommandTags()) {
            if (tag != null && tag.startsWith(TAG_PREFIX)) return true;
        }
        return false;
    }


}
