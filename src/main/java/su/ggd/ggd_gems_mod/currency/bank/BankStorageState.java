package su.ggd.ggd_gems_mod.currency.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Персистентное хранилище банка (54 слота на игрока).
 *
 * ВАЖНО:
 * - Сохраняем только непустые слоты, чтобы ItemStack.CODEC не падал на EMPTY.
 * - В Codec НЕ используем Map (избегаем проблем "Not a string").
 */
public final class BankStorageState extends PersistentState {

    public static final int SIZE = 9 * 6;

    private final Map<UUID, DefaultedList<ItemStack>> data = new HashMap<>();

    /* ------------------------------------------------------------ */
    /*  Codec модели (список записей вместо Map)                     */
    /* ------------------------------------------------------------ */

    public record SlotEntry(int slot, ItemStack stack) {
        public static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(SlotEntry::slot),
                ItemStack.CODEC.fieldOf("stack").forGetter(SlotEntry::stack)
        ).apply(i, SlotEntry::new));
    }

    public record PlayerEntry(String uuid, List<SlotEntry> items) {
        public static final Codec<PlayerEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("uuid").forGetter(PlayerEntry::uuid),
                SlotEntry.CODEC.listOf().fieldOf("items").forGetter(PlayerEntry::items)
        ).apply(i, PlayerEntry::new));
    }

    private record Root(List<PlayerEntry> players) {
        public static final Codec<Root> CODEC = RecordCodecBuilder.create(i -> i.group(
                PlayerEntry.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(Root::players)
        ).apply(i, Root::new));
    }

    public static final Codec<BankStorageState> CODEC =
            Root.CODEC.xmap(
                    BankStorageState::fromRoot,
                    BankStorageState::toRoot
            );

    public static final PersistentStateType<BankStorageState> TYPE =
            new PersistentStateType<>(
                    "ggd_gems_mod_bank_storage",
                    BankStorageState::new,
                    CODEC,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE
            );

    public BankStorageState() {}

    private static BankStorageState fromRoot(Root root) {
        BankStorageState st = new BankStorageState();

        for (PlayerEntry pe : root.players()) {
            try {
                UUID uuid = UUID.fromString(pe.uuid());
                DefaultedList<ItemStack> list = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

                for (SlotEntry se : pe.items()) {
                    int slot = se.slot();
                    if (slot < 0 || slot >= SIZE) continue;

                    ItemStack stack = se.stack();
                    if (stack == null || stack.isEmpty()) continue; // критично: не сохраняем пустые

                    list.set(slot, stack);
                }

                if (!isAllEmpty(list)) {
                    st.data.put(uuid, list);
                }
            } catch (Exception ignored) {}
        }

        return st;
    }

    private Root toRoot() {
        List<PlayerEntry> players = new ArrayList<>();

        for (var e : data.entrySet()) {
            UUID uuid = e.getKey();
            DefaultedList<ItemStack> stacks = e.getValue();
            if (uuid == null || stacks == null) continue;

            List<SlotEntry> items = new ArrayList<>();
            for (int i = 0; i < Math.min(SIZE, stacks.size()); i++) {
                ItemStack s = stacks.get(i);
                if (s == null || s.isEmpty()) continue; // критично: не записываем EMPTY
                items.add(new SlotEntry(i, s));
            }

            if (!items.isEmpty()) {
                players.add(new PlayerEntry(uuid.toString(), items));
            }
        }

        return new Root(players);
    }

    private static boolean isAllEmpty(List<ItemStack> list) {
        for (ItemStack s : list) {
            if (s != null && !s.isEmpty()) return false;
        }
        return true;
    }

    /* ------------------------------------------------------------ */

    public static BankStorageState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public DefaultedList<ItemStack> getOrCreate(ServerPlayerEntity player) {
        return data.computeIfAbsent(
                player.getUuid(),
                u -> DefaultedList.ofSize(SIZE, ItemStack.EMPTY)
        );
    }

    public void save(ServerPlayerEntity player, DefaultedList<ItemStack> stacks) {
        // cleanup: если всё пусто — удаляем игрока из state
        if (stacks == null || isAllEmpty(stacks)) {
            data.remove(player.getUuid());
        } else {
            data.put(player.getUuid(), stacks);
        }
        markDirty();
    }
}