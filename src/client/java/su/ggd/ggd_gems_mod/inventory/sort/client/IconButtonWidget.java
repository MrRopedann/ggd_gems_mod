package su.ggd.ggd_gems_mod.inventory.sort.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;

import java.util.function.Supplier;

public final class IconButtonWidget extends ButtonWidget {

    private static final net.minecraft.text.Text EMPTY_TEXT =
            MutableText.of(PlainTextContent.of(""));

    private final Supplier<ItemStack> iconSupplier;

    public IconButtonWidget(
            int x, int y, int w, int h,
            Supplier<ItemStack> iconSupplier,
            PressAction onPress
    ) {
        super(x, y, w, h, EMPTY_TEXT, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.iconSupplier = iconSupplier;
    }

    @Override
    protected void drawIcon(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        ItemStack icon = iconSupplier.get();
        if (icon == null || icon.isEmpty()) return;

        int ix = this.getX() + (this.width - 16) / 2;
        int iy = this.getY() + (this.height - 16) / 2;
        ctx.drawItem(icon, ix, iy);
    }
}
