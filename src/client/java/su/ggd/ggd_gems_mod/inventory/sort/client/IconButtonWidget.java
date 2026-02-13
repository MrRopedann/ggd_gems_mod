package su.ggd.ggd_gems_mod.inventory.sort.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;

import java.util.function.Supplier;

public final class IconButtonWidget extends ButtonWidget {

    private static final net.minecraft.text.Text EMPTY_TEXT =
            MutableText.of(PlainTextContent.of(""));

    private int ggd$baseX = 0;
    private boolean ggd$baseXSet = false;

    private final Supplier<ItemStack> iconSupplier;

    public void ggd$setBaseX(int x) {
        this.ggd$baseX = x;
        this.ggd$baseXSet = true;
    }

    public int ggd$getBaseX() {
        return this.ggd$baseX;
    }

    public boolean ggd$hasBaseX() {
        return this.ggd$baseXSet;
    }

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
        var scr = MinecraftClient.getInstance().currentScreen;
        ItemStack icon = iconSupplier.get();
        if (icon == null || icon.isEmpty()) return;

        // 1) Сначала корректируем X (как Inventory-Sorter)
        if (ggd$baseXSet) {
            int off = 0;

            // parent должен быть Screen. Если у тебя поле называется иначе — используй именно Screen ссылку.
            if (scr instanceof net.minecraft.client.gui.screen.ingame.RecipeBookScreen<?> rb) {
                var w = ((su.ggd.ggd_gems_mod.mixin.client.RecipeBookScreenAccessor) rb).ggd$getRecipeBook();
                off = (w != null && w.isOpen()) ? 77 : 0;
            }

            this.setX(ggd$baseX + off);
        }

        // 2) Потом считаем координаты и рисуем
        int ix = this.getX() + (this.width - 16) / 2;
        int iy = this.getY() + (this.height - 16) / 2;

        ctx.drawItem(icon, ix, iy);
    }


}
