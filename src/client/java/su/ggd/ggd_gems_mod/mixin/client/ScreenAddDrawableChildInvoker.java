package su.ggd.ggd_gems_mod.mixin.client;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAddDrawableChildInvoker {
    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T ggd$addDrawableChild(T element);
}
