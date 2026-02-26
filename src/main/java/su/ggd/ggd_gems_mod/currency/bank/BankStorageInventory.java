package su.ggd.ggd_gems_mod.currency.bank;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

/**
 * Inventory-обёртка над BankStorageState для одного игрока.
 */
public final class BankStorageInventory implements Inventory {

    private final BankStorageState state;
    private final ServerPlayerEntity player;
    private final DefaultedList<ItemStack> stacks;

    public BankStorageInventory(BankStorageState state, ServerPlayerEntity player) {
        this.state = state;
        this.player = player;
        this.stacks = state.getOrCreate(player);
    }

    @Override
    public int size() {
        return BankStorageState.SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : stacks) {
            if (s != null && !s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= size()) return ItemStack.EMPTY;
        ItemStack s = stacks.get(slot);
        return s == null ? ItemStack.EMPTY : s;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < 0 || slot >= size() || amount <= 0) return ItemStack.EMPTY;

        ItemStack cur = getStack(slot);
        if (cur.isEmpty()) return ItemStack.EMPTY;

        ItemStack out = cur.split(amount);
        if (cur.isEmpty()) stacks.set(slot, ItemStack.EMPTY);

        markDirty();
        return out;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot < 0 || slot >= size()) return ItemStack.EMPTY;

        ItemStack cur = getStack(slot);
        stacks.set(slot, ItemStack.EMPTY);

        markDirty();
        return cur;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size()) return;
        stacks.set(slot, stack == null ? ItemStack.EMPTY : stack);
        markDirty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    public void markDirty() {
        state.save(player, stacks);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size(); i++) stacks.set(i, ItemStack.EMPTY);
        markDirty();
    }

    // В 1.21 Inventory не объявляет onClose, поэтому без @Override
    public void onClose(PlayerEntity player) {
        markDirty();
    }
}