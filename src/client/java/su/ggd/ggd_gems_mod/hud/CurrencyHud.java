package su.ggd.ggd_gems_mod.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.currency.client.CurrencyClientState;

public final class CurrencyHud {
    private CurrencyHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        long bal = CurrencyClientState.get();
        if (bal <= 0) return;

        // Иконка пиастра (если предмет есть), иначе gold nugget
        var item = Registries.ITEM.get(Identifier.of("ggd_gems_mod", "piastre"));
        ItemStack icon = (item == null || item == Items.AIR) ? new ItemStack(Items.GOLD_NUGGET) : new ItemStack(item);

        int x = 8;
        int y = mc.getWindow().getScaledHeight() - 28; // над хотбаром
        ctx.drawItem(icon, x, y);

        String text = String.valueOf(bal);
        ctx.drawText(mc.textRenderer, text, x + 18, y + 4, 0xFFF0E68C, true);
    }
}
