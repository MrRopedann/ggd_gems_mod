package su.ggd.ggd_gems_mod.passive;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class BowTrajectoryRenderer {
    private BowTrajectoryRenderer() {}

    public static void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(BowTrajectoryRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (ClientPassiveSkills.getLevel("bow_trajectory") <= 0) return;

        if (!mc.player.isUsingItem()) return;
        ItemStack active = mc.player.getActiveItem();
        if (!(active.getItem() instanceof BowItem)) return;

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        PassiveSkillsConfig.PassiveSkillDef def =
                (cfg == null || cfg.byId == null) ? null : cfg.byId.get("bow_trajectory");

        int steps = def == null ? 60 : JsonParamUtil.getInt(def.params, "steps", 60);
        double drag = def == null ? 0.99 : JsonParamUtil.getDouble(def.params, "drag", 0.99);
        double gravity = def == null ? 0.05 : JsonParamUtil.getDouble(def.params, "gravity", 0.05);
        double minPull = def == null ? 0.10 : JsonParamUtil.getDouble(def.params, "minPull", 0.10);

        // визуал
        float outerWidth = def == null ? 4.0f : (float) JsonParamUtil.getDouble(def.params, "outerWidth", 4.0);
        float innerWidth = def == null ? 2.0f : (float) JsonParamUtil.getDouble(def.params, "innerWidth", 2.0);
        float markerSize = def == null ? 0.18f : (float) JsonParamUtil.getDouble(def.params, "markerSize", 0.18);
        float markerWidth = def == null ? 3.0f : (float) JsonParamUtil.getDouble(def.params, "markerWidth", 3.0);

        // “толщина” raycast по сущностям (увеличь, если иногда “пролетает” мимо)
        double entityRayRadius = def == null ? 0.55 : JsonParamUtil.getDouble(def.params, "entityRayRadius", 0.55);

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        int used = active.getMaxUseTime(mc.player) - mc.player.getItemUseTimeLeft();
        float pull = BowItem.getPullProgress(used);
        if (pull < (float) minPull) return;

        float g = pull * 3.0F;
        g = (g * g + g * 2.0F) / 3.0F;
        if (g > 1.0F) g = 1.0F;
        if (g < 0.1F) return;

        double speed = g * 3.0D;

        World world = mc.world;

        // направление прицеливания
        Vec3d dir = mc.player.getRotationVec(tickDelta);

        // старт ближе к руке
        Vec3d eye = mc.player.getEyePos();

        Vec3d right = dir.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 1e-6) right = new Vec3d(1, 0, 0);
        right = right.normalize();

        Arm arm = mc.player.getMainArm();
        double sideSign = (arm == Arm.RIGHT) ? 1.0 : -1.0;

        double side = 0.18 * sideSign;
        double down = 0.10;
        double forward = 0.30;

        Vec3d pos = eye
                .add(right.multiply(side))
                .add(0, -down, 0)
                .add(dir.multiply(forward));

        Vec3d vel = dir.multiply(speed);

        List<Vec3d> points = new ArrayList<>(steps + 1);
        points.add(pos);

        HitResult lastHit = null;

        Predicate<Entity> predicate = e ->
                e instanceof LivingEntity
                        && e != mc.player
                        && e.isAlive()
                        && !e.isSpectator();


        for (int i = 0; i < steps; i++) {
            Vec3d next = pos.add(vel);

            HitResult hit = raycastBlocksAndEntities(world, mc.player, pos, next, entityRayRadius, predicate);

            if (hit.getType() != HitResult.Type.MISS) {
                lastHit = hit;
                points.add(hit.getPos());
                break;
            }

            points.add(next);

            vel = vel.multiply(drag).add(0.0, -gravity, 0.0);
            pos = next;
        }

        if (points.size() < 2) return;

        Vec3d camPos = context.worldState().cameraRenderState.pos;

        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer vc = consumers.getBuffer(RenderLayers.lines());

        matrices.push();
        MatrixStack.Entry entry = matrices.peek();

        // двойная линия
        drawPolyline(vc, entry, points, camPos, outerWidth, 0, 0, 0, 170);
        drawPolyline(vc, entry, points, camPos, innerWidth, 120, 255, 120, 230);

        // маркер попадания
        Vec3d impact = (lastHit != null) ? lastHit.getPos() : points.get(points.size() - 1);
        boolean entityHit = lastHit instanceof EntityHitResult;

        if (entityHit) {
            drawImpactMarker(vc, entry, impact, camPos, markerSize, markerWidth, 255, 80, 80, 235);
        } else {
            drawImpactMarker(vc, entry, impact, camPos, markerSize, markerWidth, 255, 255, 255, 220);
        }

        matrices.pop();
    }

    /**
     * Блоки + сущности: берём ближайшее пересечение.
     * Entity hit ищем вручную по Box и пересечению отрезка с AABB.
     */
    private static HitResult raycastBlocksAndEntities(World world,
                                                      Entity owner,
                                                      Vec3d start,
                                                      Vec3d end,
                                                      double entityRadius,
                                                      Predicate<Entity> predicate) {

        HitResult blockHit = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                owner
        ));

        Vec3d effectiveEnd = (blockHit.getType() == HitResult.Type.MISS) ? end : blockHit.getPos();

        EntityHitResult entityHit = raycastEntities(world, owner, start, effectiveEnd, entityRadius, predicate);

        if (entityHit == null) return blockHit;
        if (blockHit.getType() == HitResult.Type.MISS) return entityHit;

        double bd2 = start.squaredDistanceTo(blockHit.getPos());
        double ed2 = start.squaredDistanceTo(entityHit.getPos());
        return (ed2 <= bd2) ? entityHit : blockHit;
    }

    private static EntityHitResult raycastEntities(World world,
                                                   Entity owner,
                                                   Vec3d start,
                                                   Vec3d end,
                                                   double radius,
                                                   Predicate<Entity> predicate) {

        Box search = new Box(start, end).expand(radius);

        // Сортируем по дистанции до start (дешево и стабильно), но финально берём ближайшее пересечение.
        List<Entity> list = world.getOtherEntities(owner, search, predicate);
        if (list.isEmpty()) return null;

        list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(start)));

        double bestT = Double.POSITIVE_INFINITY;
        Entity best = null;
        Vec3d bestPos = null;

        Vec3d dir = end.subtract(start);
        double len2 = dir.lengthSquared();
        if (len2 < 1e-9) return null;

        for (Entity e : list) {
            // расширяем хитбокс, чтобы “луч” имел толщину
            Box bb = e.getBoundingBox().expand(radius);

            // если старт уже внутри — считаем попадание сразу
            if (bb.contains(start)) {
                return new EntityHitResult(e, start);
            }

            Vec3d hit = intersectSegmentAabb(start, end, bb);
            if (hit == null) continue;

            // параметр t по длине (0..1)
            double t = start.squaredDistanceTo(hit) / len2;
            if (t < bestT) {
                bestT = t;
                best = e;
                bestPos = hit;
            }
        }

        return (best == null) ? null : new EntityHitResult(best, bestPos);
    }

    /**
     * Пересечение отрезка [a,b] с AABB.
     * Возвращает точку входа, или null если нет пересечения.
     */
    private static Vec3d intersectSegmentAabb(Vec3d a, Vec3d b, Box bb) {
        double tmin = 0.0;
        double tmax = 1.0;

        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;

        // X
        if (Math.abs(dx) < 1e-12) {
            if (a.x < bb.minX || a.x > bb.maxX) return null;
        } else {
            double inv = 1.0 / dx;
            double t1 = (bb.minX - a.x) * inv;
            double t2 = (bb.maxX - a.x) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return null;
        }

        // Y
        if (Math.abs(dy) < 1e-12) {
            if (a.y < bb.minY || a.y > bb.maxY) return null;
        } else {
            double inv = 1.0 / dy;
            double t1 = (bb.minY - a.y) * inv;
            double t2 = (bb.maxY - a.y) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return null;
        }

        // Z
        if (Math.abs(dz) < 1e-12) {
            if (a.z < bb.minZ || a.z > bb.maxZ) return null;
        } else {
            double inv = 1.0 / dz;
            double t1 = (bb.minZ - a.z) * inv;
            double t2 = (bb.maxZ - a.z) * inv;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return null;
        }

        // точка входа
        return new Vec3d(
                a.x + dx * tmin,
                a.y + dy * tmin,
                a.z + dz * tmin
        );
    }

    private static void drawPolyline(VertexConsumer vc,
                                     MatrixStack.Entry entry,
                                     List<Vec3d> points,
                                     Vec3d camPos,
                                     float width,
                                     int r, int g, int b, int a) {

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3d aW = points.get(i);
            Vec3d bW = points.get(i + 1);

            Vec3d aC = aW.subtract(camPos);
            Vec3d bC = bW.subtract(camPos);

            vc.vertex(entry, (float) aC.x, (float) aC.y, (float) aC.z)
                    .color(r, g, b, a)
                    .normal(entry, 0f, 1f, 0f)
                    .lineWidth(width);

            vc.vertex(entry, (float) bC.x, (float) bC.y, (float) bC.z)
                    .color(r, g, b, a)
                    .normal(entry, 0f, 1f, 0f)
                    .lineWidth(width);
        }
    }

    private static void drawImpactMarker(VertexConsumer vc,
                                         MatrixStack.Entry entry,
                                         Vec3d impactWorld,
                                         Vec3d camPos,
                                         float size,
                                         float width,
                                         int r, int g, int b, int a) {

        Vec3d p = impactWorld.subtract(camPos);

        addLine(vc, entry,
                p.x - size, p.y - size, p.z,
                p.x + size, p.y + size, p.z,
                width, r, g, b, a);

        addLine(vc, entry,
                p.x - size, p.y + size, p.z,
                p.x + size, p.y - size, p.z,
                width, r, g, b, a);

        float s2 = size * 0.75f;

        addLine(vc, entry,
                p.x - s2, p.y, p.z,
                p.x + s2, p.y, p.z,
                width, r, g, b, a);

        addLine(vc, entry,
                p.x, p.y - s2, p.z,
                p.x, p.y + s2, p.z,
                width, r, g, b, a);
    }

    private static void addLine(VertexConsumer vc,
                                MatrixStack.Entry entry,
                                double ax, double ay, double az,
                                double bx, double by, double bz,
                                float width,
                                int r, int g, int b, int a) {

        vc.vertex(entry, (float) ax, (float) ay, (float) az)
                .color(r, g, b, a)
                .normal(entry, 0f, 1f, 0f)
                .lineWidth(width);

        vc.vertex(entry, (float) bx, (float) by, (float) bz)
                .color(r, g, b, a)
                .normal(entry, 0f, 1f, 0f)
                .lineWidth(width);
    }
}
