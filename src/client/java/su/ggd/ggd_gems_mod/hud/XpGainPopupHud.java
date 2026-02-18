package su.ggd.ggd_gems_mod.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class XpGainPopupHud implements HudRenderCallback {

    private static boolean DONE = false;

    private static final int XP_BAR_WIDTH = 182;

    // Размер/стиль как у твоих чисел (можешь подстроить)
    private static final float TEXT_SCALE = 1.15f;

    // Цвет текста + обводка
    private static final int OUTLINE = 0xFF000000;
    private static final int BASE_COLOR = 0xFFFFB300; // жёлто-оранжевый

    private static int lastTotalXp = -1;
    private static final List<Popup> POPUPS = new ArrayList<>();

    public static void init() {
        if (DONE) return;
        DONE = true;

        HudRenderCallback.EVENT.register(new XpGainPopupHud());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity p = client.player;
            if (p == null) {
                lastTotalXp = -1;
                POPUPS.clear();
                return;
            }

            int now = p.totalExperience;

            if (lastTotalXp == -1) {
                lastTotalXp = now;
                return;
            }

            int diff = now - lastTotalXp;

            // показываем только получение (если тратит — не показываем)
            if (diff > 0) {
                // если опыт приходит очень часто, можно объединять в один popup:
                if (!POPUPS.isEmpty() && POPUPS.get(POPUPS.size() - 1).canMerge()) {
                    POPUPS.get(POPUPS.size() - 1).amount += diff;
                    POPUPS.get(POPUPS.size() - 1).life = 0; // освежаем
                } else {
                    POPUPS.add(new Popup(diff));
                }
            }

            lastTotalXp = now;

            Iterator<Popup> it = POPUPS.iterator();
            while (it.hasNext()) {
                Popup pop = it.next();
                pop.tick();
                if (pop.isDead()) it.remove();
            }
        });
    }

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (POPUPS.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int barX = (screenW - XP_BAR_WIDTH) / 2;
        int barY = screenH - 32 + 3;
        int centerX = barX + XP_BAR_WIDTH / 2;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);

        for (int i = 0; i < POPUPS.size(); i++) {
            Popup p = POPUPS.get(i);

            float t = p.progress();          // 0..1
            float rise = t * 42f + i * 14f;  // подъем + небольшой стэк

            int alpha = (int) (255 * (1f - t));
            int color = (alpha << 24) | (BASE_COLOR & 0x00FFFFFF);

            Text text = Text.literal("+" + p.amount);

            int w = tr.getWidth(text);

            int x = (int) ((centerX - w / 2) / TEXT_SCALE);
            int y = (int) ((barY - 14 - rise) / TEXT_SCALE);

            // обводка (4 направления)
            drawOutlined(ctx, tr, text, x, y, color);
        }

        ctx.getMatrices().popMatrix();
    }

    private static void drawOutlined(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color) {
        ctx.drawText(tr, text, x - 1, y, OUTLINE, false);
        ctx.drawText(tr, text, x + 1, y, OUTLINE, false);
        ctx.drawText(tr, text, x, y - 1, OUTLINE, false);
        ctx.drawText(tr, text, x, y + 1, OUTLINE, false);

        ctx.drawText(tr, text, x, y, color, false);
    }

    private static final class Popup {
        int amount;
        int life = 0;

        static final int MAX_LIFE = 50; // короче/длиннее — меняй

        Popup(int amount) {
            this.amount = amount;
        }

        void tick() {
            life++;
        }

        boolean isDead() {
            return life >= MAX_LIFE;
        }

        float progress() {
            return life / (float) MAX_LIFE;
        }

        boolean canMerge() {
            return life < 6; // в первые тики объединяем частые начисления
        }
    }
}
