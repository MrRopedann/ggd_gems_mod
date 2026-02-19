package su.ggd.ggd_gems_mod.client.menu.bank;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.currency.client.CurrencyClientState;
import su.ggd.ggd_gems_mod.currency.net.BankNet;

public final class BankScreen extends Screen {

    public BankScreen() {
        super(Text.literal("Банк"));
    }

    @Override
    protected void init() {
        int cx = 10;
        int y = 20;

        addDrawableChild(ButtonWidget.builder(Text.literal("Внести все пиастры из инвентаря"), b -> {
            ClientPlayNetworking.send(new BankNet.DepositAllPayload());
        }).dimensions(cx, y, 260, 20).build());
        y += 24;

        addDrawableChild(ButtonWidget.builder(Text.literal("Снять 1"), b -> withdraw(1)).dimensions(cx, y, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Снять 10"), b -> withdraw(10)).dimensions(cx + 90, y, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Снять 64"), b -> withdraw(64)).dimensions(cx + 180, y, 80, 20).build());
    }

    private void withdraw(int amount) {
        ClientPlayNetworking.send(new BankNet.WithdrawPayload(amount));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // фон не нужен — его рисует контейнерный RpgMenuScreen
        super.render(ctx, mouseX, mouseY, delta);

        long bal = CurrencyClientState.get();
        ctx.drawText(textRenderer, Text.literal("Баланс: " + bal), 10, 5, 0xFFFFFFFF, false);
    }
}
