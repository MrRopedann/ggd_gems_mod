package su.ggd.ggd_gems_mod.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class ExperienceBarTextHud implements HudRenderCallback {

    private static final int XP_BAR_WIDTH = 182;

    // 0.85 - почти ваниль, 0.75 - аккуратно, 0.65 - компактно
    private static final float TEXT_SCALE = 0.8f;

    public static void init() {
        HudRenderCallback.EVENT.register(new ExperienceBarTextHud());
    }

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;

        PlayerEntity player = mc.player;
        if (player == null) return;
        if (player.isSpectator()) return;
        if (player.getAbilities().creativeMode) return;

        int required = player.getNextLevelExperience();
        if (required <= 0) return;

        int current = MathHelper.floor(player.experienceProgress * required);

        Text leftText = Text.literal(Integer.toString(current));
        Text rightText = Text.literal(Integer.toString(required));

        TextRenderer tr = mc.textRenderer;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int barX = (screenW - XP_BAR_WIDTH) / 2;
        int barY = screenH - 32 + 3;

        // На одной высоте с уровнем, но по краям XP-бара
        int textY = barY;

        int leftX = barX + 3;
        int rightW = tr.getWidth(rightText);
        int rightX = barX + XP_BAR_WIDTH - rightW - 3;

        // ---- Масштабирование (Matrix3x2fStack) ----
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);

        int sxLeft = (int) (leftX / TEXT_SCALE);
        int sxRight = (int) (rightX / TEXT_SCALE);
        int sy = (int) (textY / TEXT_SCALE);

        int outlineColor = 0xFF000000;       // чёрная обводка
        int textColor = 0xFFFFB300;          // жёлто-оранжевый

        // ----- LEFT -----
        ctx.drawText(tr, leftText, sxLeft - 1, sy, outlineColor, false);
        ctx.drawText(tr, leftText, sxLeft + 1, sy, outlineColor, false);
        ctx.drawText(tr, leftText, sxLeft, sy - 1, outlineColor, false);
        ctx.drawText(tr, leftText, sxLeft, sy + 1, outlineColor, false);

        ctx.drawText(tr, leftText, sxLeft, sy, textColor, false);

        // ----- RIGHT -----
        ctx.drawText(tr, rightText, sxRight - 1, sy, outlineColor, false);
        ctx.drawText(tr, rightText, sxRight + 1, sy, outlineColor, false);
        ctx.drawText(tr, rightText, sxRight, sy - 1, outlineColor, false);
        ctx.drawText(tr, rightText, sxRight, sy + 1, outlineColor, false);

        ctx.drawText(tr, rightText, sxRight, sy, textColor, false);


        ctx.getMatrices().popMatrix();
    }
}
