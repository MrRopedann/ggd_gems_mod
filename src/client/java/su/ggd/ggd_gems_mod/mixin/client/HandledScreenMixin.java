package su.ggd.ggd_gems_mod.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.ggd.ggd_gems_mod.inventory.sort.client.IconButtonWidget;
import su.ggd.ggd_gems_mod.inventory.sort.client.InventorySortButtons;
import su.ggd.ggd_gems_mod.inventory.sort.client.InventorySortScreen;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> implements InventorySortScreen {

    @Unique private IconButtonWidget ggd$modeBtn;
    @Unique private IconButtonWidget ggd$sortBtn;

    @Inject(method = "init", at = @At("TAIL"))
    private void ggd$initTail(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        ScreenAddDrawableChildInvoker inv = (ScreenAddDrawableChildInvoker) (Object) this;

        if (ggd$modeBtn == null) {
            ggd$modeBtn = InventorySortButtons.createModeButton(client);
            inv.ggd$addDrawableChild(ggd$modeBtn);
        }
        if (ggd$sortBtn == null) {
            ggd$sortBtn = InventorySortButtons.createSortButton(client);
            inv.ggd$addDrawableChild(ggd$sortBtn);
        }

        InventorySortButtons.reposition((HandledScreen<?>)(Object)this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ggd$renderTail(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        InventorySortButtons.reposition((HandledScreen<?>)(Object)this);
    }

    @Override public IconButtonWidget ggd$getModeButton() { return ggd$modeBtn; }
    @Override public IconButtonWidget ggd$getSortButton() { return ggd$sortBtn; }
}
