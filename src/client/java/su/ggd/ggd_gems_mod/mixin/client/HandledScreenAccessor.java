package su.ggd.ggd_gems_mod.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x") int ggd$getX();
    @Accessor("y") int ggd$getY();

    @Accessor("backgroundWidth") int ggd$getBackgroundWidth();
    @Accessor("backgroundHeight") int ggd$getBackgroundHeight();

    @Accessor("handler") ScreenHandler ggd$getHandler();
}
