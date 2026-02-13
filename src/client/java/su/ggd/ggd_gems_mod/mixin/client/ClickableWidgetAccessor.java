package su.ggd.ggd_gems_mod.mixin.client;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClickableWidget.class)
public interface ClickableWidgetAccessor {
    @Accessor("x") int ggd$getX();
    @Accessor("y") int ggd$getY();
    @Accessor("width") int ggd$getWidth();
    @Accessor("height") int ggd$getHeight();

    @Accessor("x") void ggd$setX(int x);
    @Accessor("y") void ggd$setY(int y);
    @Accessor("width") void ggd$setWidth(int width);
    @Accessor("height") void ggd$setHeight(int height);
}
