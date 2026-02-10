package su.ggd.ggd_gems_mod.inventory.sort.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import su.ggd.ggd_gems_mod.inventory.sort.InventorySortType;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortNet;

import java.lang.reflect.Method;

public final class InventorySortButtons {
    private InventorySortButtons() {}

    private static Method ADD_DRAWABLE_CHILD;

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!isSupported(screen)) return;
            addButtons(client, screen);
        });
    }

    private static boolean isSupported(Screen screen) {
        return (screen instanceof HandledScreen<?>);
    }

    private static net.minecraft.text.Text tt(String s) {
        return MutableText.of(PlainTextContent.of(s));
    }


    private static void addButtons(MinecraftClient client, Screen screen) {
        int w = 20;
        int h = 20;
        int gap = 2;

        GuiBox box = guiBox(screen);

        int modeX;
        int sortX;
        int y;

        if (screen instanceof InventoryScreen) {
            y = box.top + 61;
            modeX = box.left + 128;
            sortX = modeX + w + gap;
        } else {
            // Y остаётся на строке "Инвентарь"
            int INVENTORY_LABEL_Y_OFFSET = 94;
            int EXTRA_UP = 5;

            y = box.top + box.bgH - INVENTORY_LABEL_Y_OFFSET - 2 - EXTRA_UP;

            // ✅ Якорим X к правому краю контейнерных слотов
            SlotAnchor anchor = getContainerSlotAnchor(screen, box);
            int rightEdge = anchor.rightEdgeX;

            // две кнопки справа от последнего слота, но внутри рамки
            sortX = rightEdge - w;          // в пределах
            modeX = sortX - (w + gap);
        }

        IconButtonWidget modeBtn = new IconButtonWidget(
                modeX, y, w, h,
                InventorySortButtons::modeIcon,
                b -> {
                    InventorySortClient.cycleMode(client, true);
                    // обновляем тултип, чтобы отображал текущий режим
                    b.setTooltip(Tooltip.of(tt("Режим: " + InventorySortClient.getCurrentSortType().name())));
                }
        );
        modeBtn.setTooltip(Tooltip.of(tt("Режим: " + InventorySortClient.getCurrentSortType().name())));

        IconButtonWidget sortBtn = new IconButtonWidget(
                sortX, y, w, h,
                () -> new ItemStack(Items.HOPPER),
                b -> {
                    if (client.player == null) return;
                    ClientPlayNetworking.send(new InventorySortNet.SortRequestPayload(
                            InventorySortClient.getCurrentSortType().toWire()
                    ));
                }
        );
        sortBtn.setTooltip(Tooltip.of(tt("Сортировать")));

        invokeAddDrawableChild(screen, modeBtn);
        invokeAddDrawableChild(screen, sortBtn);
    }

    private record GuiBox(int left, int top, int bgW, int bgH) {}

    private static GuiBox guiBox(Screen screen) {
        // Фолбек
        int fallbackW = screen.width;
        int fallbackH = screen.height;

        if (!(screen instanceof HandledScreen<?>)) {
            return new GuiBox(0, 0, fallbackW, fallbackH);
        }

        Integer bgW = firstNonNull(
                getIntField(screen, "backgroundWidth"),
                getIntField(screen, "imageWidth"),
                getIntField(screen, "field_2791"),
                getIntField(screen, "field_3390")
        );
        Integer bgH = firstNonNull(
                getIntField(screen, "backgroundHeight"),
                getIntField(screen, "imageHeight"),
                getIntField(screen, "field_2792"),
                getIntField(screen, "field_3391")
        );

        Integer left = firstNonNull(
                getIntField(screen, "x"),
                getIntField(screen, "leftPos"),
                getIntField(screen, "field_2797"),
                getIntField(screen, "field_3389")
        );
        Integer top = firstNonNull(
                getIntField(screen, "y"),
                getIntField(screen, "topPos"),
                getIntField(screen, "field_2800"),
                getIntField(screen, "field_3392")
        );


        if (bgW == null) bgW = 176;
        if (bgH == null) bgH = 166;

        if (left == null) left = (screen.width - bgW) / 2;
        if (top == null) top = (screen.height - bgH) / 2;

        return new GuiBox(left, top, bgW, bgH);
    }


    private static Integer getIntField(Object obj, String name) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(obj);
            if (v instanceof Integer i) return i;
        } catch (Throwable ignored) {}
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... vals) {
        for (T v : vals) if (v != null) return v;
        return null;
    }

    private static ItemStack modeIcon() {
        InventorySortType t = InventorySortClient.getCurrentSortType();
        return switch (t) {
            case NAME -> new ItemStack(Items.NAME_TAG);       // сортировка по имени
            case COUNT -> new ItemStack(Items.CHEST);         // количество
            case ITEM_ID -> new ItemStack(Items.PAPER);       // id/реестр (условно)
            case RARITY -> new ItemStack(Items.NETHER_STAR);  // редкость
        };
    }

    private static void invokeAddDrawableChild(Screen screen, Element element) {
        try {
            Method m = ADD_DRAWABLE_CHILD;
            if (m == null) {
                m = Screen.class.getDeclaredMethod("addDrawableChild", Element.class);
                m.setAccessible(true);
                ADD_DRAWABLE_CHILD = m;
            }
            m.invoke(screen, element);
        } catch (Throwable ignored) {
        }
    }

    private record SlotAnchor(int rightEdgeX) {}

    private static SlotAnchor getContainerSlotAnchor(Screen screen, GuiBox box) {
        // fallback: старый способ, но с безопасным отступом
        int fallbackRight = box.left + box.bgW - 8;

        ScreenHandler handler = getScreenHandler(screen);
        if (handler == null || handler.slots == null || handler.slots.isEmpty()) {
            return new SlotAnchor(fallbackRight);
        }

        // Находим самый правый слот, который относится к контейнеру (не к инвентарю игрока).
        // Без прямого доступа к playerInv на клиенте: берём самый правый среди всех слотов.
        // Для chest/shulker этого достаточно.
        int maxX = Integer.MIN_VALUE;

        for (Slot s : handler.slots) {
            if (s == null) continue;

            // Slot.x / Slot.y в контейнере — координаты относительно GUI.
            // В 1.21+ поля обычно называются x/y и публичные.
            int sx = s.x;
            if (sx > maxX) maxX = sx;
        }

        if (maxX == Integer.MIN_VALUE) {
            return new SlotAnchor(fallbackRight);
        }

        // right edge = left + slotX + 16 (ширина слота) + небольшой паддинг
        int rightEdge = box.left + maxX + 16;
        // чтобы не вылезать за рамку
        int clamp = box.left + box.bgW - 6;
        if (rightEdge > clamp) rightEdge = clamp;

        return new SlotAnchor(rightEdge);
    }

    private static ScreenHandler getScreenHandler(Screen screen) {
        // HandledScreen имеет protected field handler (name varies). Достаём reflection.
        try {
            // частые имена
            ScreenHandler h = (ScreenHandler) getObjField(screen, "handler");
            if (h != null) return h;
            h = (ScreenHandler) getObjField(screen, "screenHandler");
            if (h != null) return h;
            h = (ScreenHandler) getObjField(screen, "field_2794");
            return h;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getObjField(Object obj, String name) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }

}
