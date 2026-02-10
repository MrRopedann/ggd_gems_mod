package su.ggd.ggd_gems_mod.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import su.ggd.ggd_gems_mod.combat.DamagePopups;
import su.ggd.ggd_gems_mod.config.NpcTradersConfigManager;
import su.ggd.ggd_gems_mod.npc.GemTraderNpc;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;



import java.lang.reflect.Method;
import java.util.Deque;

public final class MobHealthBarHud implements HudRenderCallback {

    private static final int BAR_WIDTH = 54;
    private static final int BAR_HEIGHT = 7;

    private static final double MAX_DISTANCE = 12.0;
    private static final double FADE_START  = 8.0;
    private static final double FADE_END    = 12.0;

    // scale по дистанции (близко крупнее, далеко меньше)
    private static final float SCALE_NEAR = 1.00f;
    private static final float SCALE_FAR  = 0.55f;

    private static final float TEXT_SCALE = 0.80f; // чтобы “5/10” точно помещалось внутрь
    private static final long POPUP_LIFE_MS = 2200;
    private static final float POPUP_SPREAD_X = 16f;   // максимальный разлёт по X (px на экране)
    private static final float POPUP_SPREAD_JITTER = 3f; // небольшая дрожь (опционально)


    private static final int SCREEN_MARGIN = 40;

    public static void init() {
        HudRenderCallback.EVENT.register(new MobHealthBarHud());
    }

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        float tick = tickCounter.getTickProgress(false);

        Entity camEntity = mc.getCameraEntity();
        if (camEntity == null) return;

        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

        float yaw = camEntity.getYaw(tick);
        float pitch = camEntity.getPitch(tick);

        Vec3d forward = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d worldUp = new Vec3d(0, 1, 0);

        Vec3d right = forward.crossProduct(worldUp);
        if (right.lengthSquared() < 1.0e-6) return;
        right = right.normalize();

        Vec3d up = right.crossProduct(forward).normalize();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        double aspect = screenW / (double) screenH;

        double fovDeg = resolveFovDeg(mc, tick);
        double fovRad = Math.toRadians(MathHelper.clamp((float) fovDeg, 10f, 170f));
        double tanHalfFov = Math.tan(fovRad * 0.5);

        double maxDistSq = MAX_DISTANCE * MAX_DISTANCE;

        for (LivingEntity e : mc.world.getEntitiesByClass(
                LivingEntity.class,
                mc.player.getBoundingBox().expand(MAX_DISTANCE),
                ent -> ent.isAlive()
        )) {
            if (e == mc.player) continue;

            Entity selfCam = mc.getCameraEntity();

            if (selfCam != null && e == selfCam) continue;

            // === НЕ рисуем HUD на NPC-торговцах по маркеру в CustomName ===
            String custom = (e.getCustomName() != null) ? e.getCustomName().getString() : "";
            //String display = (e.getDisplayName() != null) ? e.getDisplayName().getString() : "";
            String display = e.getDisplayName().getString();
            if (NpcTradersConfigManager.isNpcName(display)) {
                continue;
            }

            if (custom.contains("ggd_trader:") || display.contains("ggd_trader:")) {
                continue;
            }
/*
            if (e instanceof VillagerEntity || e instanceof WanderingTraderEntity) {
                System.out.println("HUD check: custom='" + custom + "' display='" + display + "'");
            }
*/

            double distSq = mc.player.squaredDistanceTo(e);
            if (distSq > maxDistSq) continue;

            // если хочешь ВСЕГДА показывать даже сквозь стены — убери эту строку
            if (!mc.player.canSee(e)) continue;

            Vec3d p = e.getLerpedPos(tick).add(0, e.getHeight() + 0.45, 0);
            Vec3d rel = p.subtract(camPos);

            double cx = rel.dotProduct(right);
            double cy = rel.dotProduct(up);
            double cz = rel.dotProduct(forward);
            if (cz <= 0.05) continue;

            double ndcX = (cx / (cz * tanHalfFov)) / aspect;
            double ndcY = (cy / (cz * tanHalfFov));

            if (ndcX < -1.15 || ndcX > 1.15 || ndcY < -1.15 || ndcY > 1.15) continue;

            int x = (int) ((ndcX * 0.5 + 0.5) * screenW);
            int y = (int) ((-ndcY * 0.5 + 0.5) * screenH);

            if (x < -SCREEN_MARGIN || x > screenW + SCREEN_MARGIN) continue;
            if (y < -SCREEN_MARGIN || y > screenH + SCREEN_MARGIN) continue;

            double dist = Math.sqrt(distSq);
            float alpha = computeFadeAlpha(dist);
            if (alpha <= 0.01f) continue;

            float barScale = computeBarScale(dist);

            drawBarAndText(ctx, x, y, e, alpha, barScale);
            drawDamagePopups(ctx, x, y, e, alpha, barScale);
        }
    }


    private static float computeFadeAlpha(double dist) {
        if (dist <= FADE_START) return 1.0f;
        if (dist >= FADE_END) return 0.0f;
        double t = (dist - FADE_START) / (FADE_END - FADE_START);
        t = t * t * (3.0 - 2.0 * t); // smoothstep
        return (float) (1.0 - t);
    }

    private static float computeBarScale(double dist) {
        double t = MathHelper.clamp((float)((dist - 2.0) / Math.max(0.001, (FADE_END - 2.0))), 0f, 1f);
        // far -> меньше
        return (float) (SCALE_NEAR + (SCALE_FAR - SCALE_NEAR) * t);
    }

    private static void drawBarAndText(DrawContext ctx, int x, int y, LivingEntity e, float alpha, float barScale) {
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        float hpF = Math.max(0f, e.getHealth());
        float maxF = Math.max(0.1f, e.getMaxHealth());
        if (maxF <= 0f) return;

        int armor = Math.max(0, e.getArmor());
        double atk = estimateAttackDamage(e); // ⚔

        float pct = MathHelper.clamp(hpF / maxF, 0f, 1f);

        int w = Math.max(18, Math.round(BAR_WIDTH * barScale));
        int h = Math.max(4, Math.round(BAR_HEIGHT * barScale));
        int filled = (int) (w * pct);

        int left = x - w / 2;
        int top = y;

        int aBg = (int) (alpha * 170);
        int aFg = (int) (alpha * 255);
        int aText = (int) (alpha * 255);

        int bg = (aBg << 24) | 0x000000;
        int fg = (aFg << 24) | 0xCC2222; // HP красный
        int textColor = (aText << 24) | 0xFFFFFF;

        // ===== имя + (броня, атака) над полосой =====
        String name = e.getDisplayName().getString();
        String suffix = buildStatsSuffix(armor, atk);
        String title = name + suffix;
        drawScaledText(ctx, tr, title, x, top - (int)(10 * barScale), textColor, barScale * 0.85f, true);

        // ===== полоса HP =====
        ctx.fill(left, top, left + w, top + h, bg);
        ctx.fill(left, top, left + filled, top + h, fg);

        // ===== HP текст внутри (float) =====
        String inside = formatHp(hpF) + "/" + formatHp(maxF);
        drawScaledText(ctx, tr, inside, x, top + h/2 - 4, textColor, barScale * TEXT_SCALE, true);
    }

    private static String formatHp(float v) {
        // 1 знак после запятой, но без ".0" если целое
        float rounded = Math.round(v * 10f) / 10f;

        // если реально > 0, но округление дало 0.0 — показываем 0.1
        if (v > 0f && rounded <= 0f) rounded = 0.1f;

        // убрать .0 у целых
        if (Math.abs(rounded - Math.round(rounded)) < 1e-4f) {
            return Integer.toString(Math.round(rounded));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", rounded);
    }


    private static String buildStatsSuffix(int armor, double atk) {
        // иконки: 🛡 ⚔
        // можно заменить на "ARM" / "ATK", если эмодзи не нравятся
        String atkS = trim((float) atk);
        return " (\uD83D\uDEE1 " + armor + " \u2694 " + atkS + ")";
    }

    private static double estimateAttackDamage(LivingEntity e) {

        // Без хардкода предметов: берём “типичный” урон сущности.
        // Для игроков и мобов в ваниле корректно учитывает атрибут ATTACK_DAMAGE.
        var inst = e.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
        if (inst == null) return 0.0;
        return Math.max(0.0, inst.getValue());
    }


    private static void drawDamagePopups(DrawContext ctx, int x, int y, LivingEntity e, float alpha, float barScale) {
        Deque<DamagePopups.Popup> q = DamagePopups.get(e.getId());
        if (q == null || q.isEmpty()) return;

        long now = System.currentTimeMillis();
        DamagePopups.cleanup(e.getId(), now, POPUP_LIFE_MS);

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        int i = 0;
        for (DamagePopups.Popup p : q) {
            float t = (now - p.startMs) / (float) POPUP_LIFE_MS;
            t = MathHelper.clamp(t, 0f, 1f);

            float rise = (26f * t) * barScale;
            float fade = 1f - (t * t); // медленнее тухнет в начале
            int a = (int) (255 * fade * alpha);

            int color = (a << 24) | 0xFF4444; // красный урон

            String dmg = "-" + trim(p.amount);

            int baseY = y - (int)(14 * barScale);
            // stable random per popup
            long seed = ((long) e.getId() * 341873128712L) ^ p.startMs ^ (i * 0x9E3779B97F4A7C15L);

            // [-1..1]
            float rnd = hash01(seed) * 2f - 1f;

            // базовый разлёт
            float spread = rnd * POPUP_SPREAD_X * barScale;

            // небольшая дрожь (по желанию) — чуть меняется со временем, но не дергается резко
            float jitter = (hash01(seed ^ 0xBADC0FFEE0DDF00DL) * 2f - 1f) * POPUP_SPREAD_JITTER * barScale;

            // чтобы в начале было почти по центру, а потом разлеталось сильнее
            float xOff = lerp(0f, spread, t) + jitter * (t);
            drawScaledText(ctx, tr, dmg, x + (int) xOff, (int)(baseY - rise - (i * 7f * barScale)), color, barScale * 0.95f, true);


            i++;
            if (i >= 3) break;
        }
    }

    /**
     * Рисуем текст по центру (cx, cy) с масштабом scale.
     * Важное: делаем через матрицы DrawContext, чтобы реально уменьшался шрифт.
     */
    private static void drawScaledText(DrawContext ctx, TextRenderer tr, String s, int cx, int cy, int color, float scale, boolean shadow) {
        if (scale <= 0.01f) return;

        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(cx, cy);
        matrices.scale(scale, scale);

        int w = tr.getWidth(s);
        // центрируем относительно (0,0) уже после scale
        ctx.drawText(tr, s, -w / 2, 0, color, shadow);

        matrices.popMatrix();
    }

    private static String trim(float v) {
        String s = String.format(java.util.Locale.US, "%.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static double resolveFovDeg(MinecraftClient mc, float tick) {
        double def = 70.0;
        Object gr = mc.gameRenderer;
        Object cam = mc.gameRenderer.getCamera();

        try {
            for (Method m : gr.getClass().getMethods()) {
                if (!m.getName().equals("getFov")) continue;
                Class<?>[] p = m.getParameterTypes();

                if (p.length == 3) {
                    Object r = m.invoke(gr, cam, tick, true);
                    if (r instanceof Number n) return n.doubleValue();
                } else if (p.length == 2) {
                    Object r = m.invoke(gr, cam, tick);
                    if (r instanceof Number n) return n.doubleValue();
                } else if (p.length == 1) {
                    Object r = m.invoke(gr, tick);
                    if (r instanceof Number n) return n.doubleValue();
                }
            }
        } catch (Throwable ignored) {}

        return def;
    }

    private static float hash01(long x) {
        x ^= (x >>> 33);
        x *= 0xff51afd7ed558ccdL;
        x ^= (x >>> 33);
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= (x >>> 33);
        // [0..1)
        return (x & 0xFFFFFF) / (float) 0x1000000;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
