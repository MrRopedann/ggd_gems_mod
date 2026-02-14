package su.ggd.ggd_gems_mod.inventory.sort.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortNet;
import su.ggd.ggd_gems_mod.mixin.client.HandledScreenAccessor;

import java.util.List;

import static java.lang.Math.clamp;

public final class InventorySortButtons {
    private InventorySortButtons() {}

    private record Buttons(IconButtonWidget mode, IconButtonWidget sort, int gap) {}
    private record Anchor(int modeX, int sortX, int y) {}
    private record GuiBox(int left, int top, int bgW, int bgH) {}
    private record SlotLayout(int playerInvTopY, int playerInvRightX) {}

    // Только для окна инвентаря игрока (как на твоём скрине)
    private static final int INVENTORY_SCREEN_Y_OFFSET = 20;
    private static final int CHEST_SCREEN_Y_OFFSET = 4; // подбери значение


    public static void init() {
        // Скролл только по наведённой кнопке (как Inventory-Sorter)
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.afterMouseScroll(screen).register((scr, x, y, hAmount, vAmount, consumed) -> {
                if (!(scr instanceof HandledScreen<?> hs)) return false;
                if (!(hs instanceof InventorySortScreen iss)) return false;

                IconButtonWidget mode = iss.ggd$getModeButton();
                if (mode != null && mode.visible && mode.isHovered()) {
                    mode.mouseScrolled(x, y, vAmount, hAmount);
                    return true;
                }

                IconButtonWidget sort = iss.ggd$getSortButton();
                if (sort != null && sort.visible && sort.isHovered()) {
                    sort.mouseScrolled(x, y, vAmount, hAmount);
                    return true;
                }

                return false;
            });
        });
    }

    public static IconButtonWidget createModeButton(MinecraftClient client) {
        int size = 20;

        IconButtonWidget modeBtn = new IconButtonWidget(
                0, 0, size, size,
                InventorySortButtons::modeIcon,
                b -> {
                    InventorySortClient.cycleMode(client, true);
                    b.setTooltip(Tooltip.of(tt("Режим: " + InventorySortClient.getCurrentSortType().name())));
                }
        );

        modeBtn.setTooltip(Tooltip.of(tt("Режим: " + InventorySortClient.getCurrentSortType().name())));
        return modeBtn;
    }

    public static IconButtonWidget createSortButton(MinecraftClient client) {
        int size = 20;

        IconButtonWidget sortBtn = new IconButtonWidget(
                0, 0, size, size,
                () -> new ItemStack(Items.HOPPER),
                b -> {
                    if (client.player == null) return;
                    ClientPlayNetworking.send(new InventorySortNet.SortRequestPayload(
                            InventorySortClient.getCurrentSortType().toWire()
                    ));
                }
        );

        sortBtn.setTooltip(Tooltip.of(tt("Сортировать")));
        return sortBtn;
    }

    /** Вызывается из HandledScreenMixin#render (TAIL) */
    public static void reposition(HandledScreen<?> screen) {
        if (!(screen instanceof InventorySortScreen iss)) return;

        IconButtonWidget mode = iss.ggd$getModeButton();
        IconButtonWidget sort = iss.ggd$getSortButton();
        if (mode == null || sort == null) return;

        Buttons btns = new Buttons(mode, sort, 2);
        updateLayout(MinecraftClient.getInstance(), screen, btns);
    }

    private static Text tt(String s) {
        return MutableText.of(PlainTextContent.of(s));
    }

    private static void updateLayout(MinecraftClient client, HandledScreen<?> screen, Buttons btns) {
        if (client == null) return;

        GuiBox box = guiBox(screen);
        SlotLayout layout = computeLayout(screen);

        int padding = 4;
        int w = 20, h = 20;

        Anchor pos = anchorAbovePlayerInventoryRight(box, layout, w, h, btns.gap, padding);

        int newY = pos.y;

        if (screen instanceof InventoryScreen) {
            // Инвентарь игрока
            newY = pos.y + INVENTORY_SCREEN_Y_OFFSET;
        }
        else if (screen instanceof net.minecraft.client.gui.screen.ingame.GenericContainerScreen) {
            // Сундук
            newY = pos.y + CHEST_SCREEN_Y_OFFSET;
        }

        int minY = box.top + padding;
        int maxY = box.top + box.bgH - h - padding;
        newY = clamp(newY, minY, maxY);

        pos = new Anchor(pos.modeX, pos.sortX, newY);

        // ВАЖНО: сохраняем baseX — X дальше двигается внутри IconButtonWidget
        btns.mode.ggd$setBaseX(pos.modeX);
        btns.sort.ggd$setBaseX(pos.sortX);

        btns.mode.setY(pos.y);
        btns.sort.setY(pos.y);

        btns.mode.setWidth(w);
        btns.mode.setHeight(h);

        btns.sort.setWidth(w);
        btns.sort.setHeight(h);

        btns.mode.setTooltip(
                Tooltip.of(tt("Режим: " + InventorySortClient.getCurrentSortType().name()))
        );
    }


    private static Anchor anchorAbovePlayerInventoryRight(
            GuiBox box,
            SlotLayout layout,
            int w, int h,
            int gap,
            int padding
    ) {
        int invTopAbs = (layout.playerInvTopY != Integer.MAX_VALUE)
                ? (box.top + layout.playerInvTopY)
                : (box.top + box.bgH);

        int y = invTopAbs - h - 2;

        int rightEdgeAbs = (layout.playerInvRightX != Integer.MIN_VALUE)
                ? (box.left + layout.playerInvRightX + 16)
                : (box.left + box.bgW - padding);

        int baseX = rightEdgeAbs - (2 * w + gap);

        int minX = box.left + padding;
        int maxX = box.left + box.bgW - (2 * w + gap) - padding;
        baseX = clamp(baseX, minX, maxX);

        y = clamp(y, box.top + padding, box.top + box.bgH - h - padding);

        return new Anchor(baseX, baseX + w + gap, y);
    }

    private static SlotLayout computeLayout(HandledScreen<?> screen) {
        ScreenHandler handler = ((HandledScreenAccessor)(Object)screen).ggd$getHandler();
        List<Slot> slots = handler.slots;
        if (slots == null || slots.isEmpty()) return new SlotLayout(Integer.MAX_VALUE, Integer.MIN_VALUE);

        int n = slots.size();
        int start = Math.max(0, n - 36);

        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for (int i = start; i < n; i++) {
            Slot s = slots.get(i);
            if (s == null) continue;
            if (s.y < minY) minY = s.y;
            if (s.x > maxX) maxX = s.x;
        }

        return new SlotLayout(minY, maxX);
    }

    private static GuiBox guiBox(HandledScreen<?> screen) {
        HandledScreenAccessor a = (HandledScreenAccessor) (Object) screen;
        return new GuiBox(
                a.ggd$getX(),
                a.ggd$getY(),
                a.ggd$getBackgroundWidth(),
                a.ggd$getBackgroundHeight()
        );
    }

    private static ItemStack modeIcon() {
        return new ItemStack(Items.COMPASS);
    }
}
