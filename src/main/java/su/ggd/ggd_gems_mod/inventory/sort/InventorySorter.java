package su.ggd.ggd_gems_mod.inventory.sort;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class InventorySorter {
    private InventorySorter() {}

    public static void sortCurrentScreen(ServerPlayerEntity player, InventorySortType type) {
        if (player == null) return;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return;

        if (handler instanceof PlayerScreenHandler) {
            sortPlayerMainInventory(handler, player.getInventory(), type);
            handler.sendContentUpdates();
            return;
        }

        // ✅ Сундуки/бочки и т.п.
        if (handler instanceof GenericContainerScreenHandler) {
            sortContainerOnly(handler, player.getInventory(), type);
            handler.sendContentUpdates();
            return;
        }

        // ✅ Шалкер (у него отдельный handler)
        if (handler instanceof ShulkerBoxScreenHandler) {
            sortContainerOnly(handler, player.getInventory(), type);
            handler.sendContentUpdates();
        }
    }

    private static void sortPlayerMainInventory(ScreenHandler handler, Inventory playerInv, InventorySortType type) {
        List<Integer> slotIds = new ArrayList<>();

        // 36 слотов: 0..35 (хотбар+основной), без брони/оффхэнда/крафта
        for (int invIndex = 0; invIndex < 36; invIndex++) {
            Slot slot = findPlayerInventorySlot(handler, playerInv, invIndex);
            if (slot == null) continue;
            int slotId = handler.slots.indexOf(slot);
            if (slotId >= 0) slotIds.add(slotId);
        }

        sortSlots(handler, slotIds, type);
    }

    private static void sortContainerOnly(ScreenHandler handler, Inventory playerInv, InventorySortType type) {
        List<Integer> slotIds = new ArrayList<>();

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);
            if (slot == null) continue;

            // контейнерные слоты — всё, что не playerInv
            if (slot.inventory != playerInv) slotIds.add(i);
        }

        sortSlots(handler, slotIds, type);
    }

    private static void sortSlots(ScreenHandler handler, List<Integer> slotIds, InventorySortType type) {
        if (slotIds.size() <= 1) return;

        slotIds.sort(Integer::compareTo);

        List<ItemStack> collected = new ArrayList<>(slotIds.size());
        for (int slotId : slotIds) {
            Slot slot = handler.slots.get(slotId);
            collected.add(slot.getStack().copy());
            slot.setStack(ItemStack.EMPTY);
        }

        List<ItemStack> nonEmpty = new ArrayList<>();
        for (ItemStack s : collected) {
            if (s != null && !s.isEmpty()) nonEmpty.add(s);
        }

        nonEmpty.sort(stackComparator(type));

        // ✅ ВАЖНО: после сортировки — стакаем одинаковые
        List<ItemStack> merged = mergeStacks(nonEmpty);

        int out = 0;
        for (ItemStack s : merged) {
            if (out >= slotIds.size()) break;
            int slotId = slotIds.get(out++);
            handler.slots.get(slotId).setStack(s);
        }
        while (out < slotIds.size()) {
            int slotId = slotIds.get(out++);
            handler.slots.get(slotId).setStack(ItemStack.EMPTY);
        }
    }

    /** Сливает одинаковые стаки (items + components) до maxCount. */
    private static List<ItemStack> mergeStacks(List<ItemStack> input) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : input) {
            if (s == null || s.isEmpty()) continue;

            // не стакается -> просто добавляем
            if (!s.isStackable() || s.getMaxCount() <= 1) {
                out.add(s);
                continue;
            }

            ItemStack cur = s.copy();
            while (!cur.isEmpty()) {
                if (!out.isEmpty()) {
                    ItemStack last = out.get(out.size() - 1);
                    if (canStackTogether(last, cur) && last.getCount() < last.getMaxCount()) {
                        int space = last.getMaxCount() - last.getCount();
                        int move = Math.min(space, cur.getCount());
                        last.increment(move);
                        cur.decrement(move);
                        if (cur.isEmpty()) break;
                        continue;
                    }
                }

                // если не удалось влить в последний — кладем новый стак (возможно >max, но на всякий случай режем)
                int max = cur.getMaxCount();
                if (cur.getCount() > max) {
                    ItemStack part = cur.copy();
                    part.setCount(max);
                    out.add(part);
                    cur.decrement(max);
                } else {
                    out.add(cur);
                    break;
                }
            }
        }
        return out;
    }

    private static Slot findPlayerInventorySlot(ScreenHandler handler, Inventory playerInv, int playerInvIndex) {
        for (Slot slot : handler.slots) {
            if (slot == null) continue;
            if (slot.inventory == playerInv && slot.getIndex() == playerInvIndex) return slot;
        }
        return null;
    }

    private static boolean canStackTogether(ItemStack a, ItemStack b) {
        // Совпадение предмета + всех компонентов (включая кастомные, энчанты и т.п.)
        return a.isStackable()
                && b.isStackable()
                && ItemStack.areItemsAndComponentsEqual(a, b);
    }

    private static Comparator<ItemStack> stackComparator(InventorySortType type) {
        Comparator<ItemStack> base = switch (type) {
            case ITEM_ID -> Comparator.comparing(s ->
                    Registries.ITEM.getId(s.getItem()).toString(), String.CASE_INSENSITIVE_ORDER
            );
            case NAME -> Comparator.comparing(s ->
                    s.getName().getString().toLowerCase(Locale.ROOT)
            );
            case COUNT -> Comparator.<ItemStack>comparingInt(ItemStack::getCount).reversed();
            case RARITY -> Comparator.comparingInt(s -> s.getRarity().ordinal());
        };

        return base
                .thenComparing(s -> Registries.ITEM.getId(s.getItem()).toString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
    }
}
