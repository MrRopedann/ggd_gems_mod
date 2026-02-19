package su.ggd.ggd_gems_mod.client.menu.trader;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.currency.client.CurrencyClientState;
import su.ggd.ggd_gems_mod.npc.client.TraderClientState;
import su.ggd.ggd_gems_mod.npc.net.NpcTraderNet;

import java.util.ArrayList;
import java.util.List;

public final class TraderScreen extends Screen {
    private static final int ROW_H = 24;

    private static final int LEFT_PAD = 10;
    private static final int TOP = 30;

    private static final int ICON_X = LEFT_PAD;
    private static final int ICON_SIZE = 16;

    private static final int TEXT_X = ICON_X + ICON_SIZE + 6;

    private int scroll = 0;
    private int visibleRows = 1;
    private int maxScroll = 0;

    private final List<ButtonWidget> buyButtons = new ArrayList<>();

    public TraderScreen() {
        super(Text.literal("Торговля"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    // В твоей версии Screen.resize принимает (int, int)
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        rebuild();
    }

    private void rebuild() {
        for (ButtonWidget b : buyButtons) remove(b);
        buyButtons.clear();

        var trades = TraderClientState.trades();

        visibleRows = Math.max(1, (this.height - (TOP + 10)) / ROW_H);
        maxScroll = Math.max(0, trades.size() - visibleRows);
        scroll = clamp(scroll, 0, maxScroll);

        int y = TOP;
        int start = scroll;
        int end = Math.min(trades.size(), start + visibleRows);

        for (int i = start; i < end; i++) {
            final int tradeIndex = i;

            ButtonWidget btn = ButtonWidget.builder(Text.literal("Купить"), b -> {
                String traderId = TraderClientState.traderId();
                if (traderId == null || traderId.isBlank()) return;
                ClientPlayNetworking.send(new NpcTraderNet.BuyPayload(traderId, tradeIndex));
            }).dimensions(this.width - 90, y + 2, 80, 20).build();

            addDrawableChild(btn);
            buyButtons.add(btn);

            y += ROW_H;
        }

        updateButtons();
    }

    private void updateButtons() {
        long bal = CurrencyClientState.get();
        var trades = TraderClientState.trades();
        int start = scroll;

        for (int i = 0; i < buyButtons.size(); i++) {
            int tradeIndex = start + i;
            if (tradeIndex < 0 || tradeIndex >= trades.size()) {
                buyButtons.get(i).active = false;
                continue;
            }
            long price = trades.get(tradeIndex).price();
            buyButtons.get(i).active = bal >= price;
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // фон без blur
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        super.render(ctx, mouseX, mouseY, delta);

        String name = TraderClientState.traderName();
        if (name == null || name.isBlank()) name = "Торговец";

        ctx.drawText(textRenderer, Text.literal(name), LEFT_PAD, 10, 0xFFFFFFFF, false);

        long bal = CurrencyClientState.get();
        ctx.drawText(textRenderer, Text.literal("Баланс: " + bal), LEFT_PAD, 20, 0xFFDDDDDD, false);

        var trades = TraderClientState.trades();
        if (trades.isEmpty()) {
            ctx.drawText(textRenderer, Text.literal("Нет товаров"), LEFT_PAD, 50, 0xFFFF7777, false);
            return;
        }

        int y = TOP;
        int start = scroll;
        int end = Math.min(trades.size(), start + visibleRows);

        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int i = start; i < end; i++) {
            var t = trades.get(i);
            ItemStack stack = t.stack();

            // Иконка
            int iconY = y + (ROW_H - ICON_SIZE) / 2;
            ctx.drawItem(stack, ICON_X, iconY);

            // Оверлей количества/прочности (вместо drawItemInSlot)
            ctx.drawStackOverlay(this.textRenderer, stack, ICON_X, iconY);

            // Текст
            String line = stack.getName().getString() + " x" + stack.getCount() + " — " + t.price();
            ctx.drawText(textRenderer, Text.literal(line), TEXT_X, y + 6, 0xFFFFFFFF, false);

            // hover
            int rowLeft = ICON_X;
            int rowRight = this.width - 10;
            int rowTop = y;
            int rowBottom = y + ROW_H;

            if (mouseX >= rowLeft && mouseX <= rowRight && mouseY >= rowTop && mouseY <= rowBottom) {
                hoveredStack = stack;
            }

            y += ROW_H;
        }

        // Tooltip: у твоей версии нужен MinecraftClient
        if (!hoveredStack.isEmpty()) {
            MinecraftClient mc = this.client;
            if (mc != null) {
                ctx.drawTooltip(this.textRenderer, getTooltipFromItem(mc, hoveredStack), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        int dir = verticalAmount > 0 ? -1 : (verticalAmount < 0 ? 1 : 0);
        if (dir == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        int next = clamp(scroll + dir, 0, maxScroll);
        if (next != scroll) {
            scroll = next;
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
