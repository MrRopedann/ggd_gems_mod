package su.ggd.ggd_gems_mod.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class XpGainPopupHud {
    private XpGainPopupHud() {}

    private static final int XP_BAR_WIDTH = 182;
    private static final float TEXT_SCALE = 1.15f;

    private static final int OUTLINE = 0xFF000000;
    private static final int BASE_COLOR = 0xFFFFB300;

    private static int lastTotalXp = -1;
    private static final List<Popup> POPUPS = new ArrayList<>();

    public static void tick(MinecraftClient client) {
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

        if (diff > 0) {
            if (!POPUPS.isEmpty() && POPUPS.get(POPUPS.size() - 1).canMerge()) {
                Popup last = POPUPS.get(POPUPS.size() - 1);
                last.amount += diff;
                last.life = 0;
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
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
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

            float t = p.progress();
            float rise = t * 42f + i * 14f;

            int alpha = (int) (255 * (1f - t));
            int color = (alpha << 24) | (BASE_COLOR & 0x00FFFFFF);

            Text text = Text.literal("+" + p.amount);
            int w = tr.getWidth(text);

            int x = (int) ((centerX - w / 2) / TEXT_SCALE);
            int y = (int) ((barY - 14 - rise) / TEXT_SCALE);

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
        static final int MAX_LIFE = 50;

        Popup(int amount) { this.amount = amount; }
        void tick() { life++; }
        boolean isDead() { return life >= MAX_LIFE; }
        float progress() { return life / (float) MAX_LIFE; }
        boolean canMerge() { return life < 6; }
    }
}
