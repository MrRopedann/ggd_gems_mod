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

import java.util.*;

/**
 * Персистентное хранилище предметов банка.
 * 54 слота (как большой сундук 9x6) на игрока.
 *
 * Ключ: UUID игрока (основной).
 * Fallback: имя игрока (для случаев, когда UUID меняется между входами).
 */
public final class BankStorageState extends PersistentState {

    public static final int SIZE = 9 * 6;

    // основное хранилище (UUID -> предметы)
    private final Map<UUID, DefaultedList<ItemStack>> byUuid = new HashMap<>();
    // fallback хранилище (name -> предметы) для миграции
    private final Map<String, DefaultedList<ItemStack>> byName = new HashMap<>();

    private static final Codec<List<ItemStack>> STACKS_CODEC = ItemStack.CODEC.listOf();
    private static final Codec<Map<String, List<ItemStack>>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, STACKS_CODEC);

    public static final Codec<BankStorageState> CODEC = RecordCodecBuilder.create(i -> i.group(
            MAP_CODEC.optionalFieldOf("byUuid", Map.of()).forGetter(BankStorageState::encodeByUuid),
            MAP_CODEC.optionalFieldOf("byName", Map.of()).forGetter(BankStorageState::encodeByName)
    ).apply(i, BankStorageState::newFromCodec));

    public static final PersistentStateType<BankStorageState> TYPE =
            new PersistentStateType<>(
                    "ggd_gems_mod_bank_storage",
                    BankStorageState::new,
                    CODEC,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE
            );

    public BankStorageState() {}

    private static BankStorageState newFromCodec(Map<String, List<ItemStack>> rawUuid,
                                                 Map<String, List<ItemStack>> rawName) {
        BankStorageState st = new BankStorageState();

        if (rawUuid != null && !rawUuid.isEmpty()) {
            for (var e : rawUuid.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(e.getKey());
                    DefaultedList<ItemStack> list = decodeList(e.getValue());
                    if (!isAllEmpty(list)) st.byUuid.put(uuid, list);
                } catch (Exception ignored) {}
            }
        }

        if (rawName != null && !rawName.isEmpty()) {
            for (var e : rawName.entrySet()) {
                String name = e.getKey();
                if (name == null || name.isBlank()) continue;
                DefaultedList<ItemStack> list = decodeList(e.getValue());
                if (!isAllEmpty(list)) st.byName.put(name, list);
            }
        }

        return st;
    }

    private Map<String, List<ItemStack>> encodeByUuid() {
        HashMap<String, List<ItemStack>> out = new HashMap<>();
        for (var e : byUuid.entrySet()) {
            UUID uuid = e.getKey();
            DefaultedList<ItemStack> list = e.getValue();
            if (uuid == null || list == null) continue;
            if (isAllEmpty(list)) continue;
            out.put(uuid.toString(), encodeList(list));
        }
        return out;
    }

    private Map<String, List<ItemStack>> encodeByName() {
        HashMap<String, List<ItemStack>> out = new HashMap<>();
        for (var e : byName.entrySet()) {
            String name = e.getKey();
            DefaultedList<ItemStack> list = e.getValue();
            if (name == null || name.isBlank() || list == null) continue;
            if (isAllEmpty(list)) continue;
            out.put(name, encodeList(list));
        }
        return out;
    }

    private static DefaultedList<ItemStack> decodeList(List<ItemStack> raw) {
        DefaultedList<ItemStack> list = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        if (raw == null || raw.isEmpty()) return list;

        for (int i = 0; i < Math.min(SIZE, raw.size()); i++) {
            ItemStack s = raw.get(i);
            list.set(i, s == null ? ItemStack.EMPTY : s);
        }
        return list;
    }

    private static List<ItemStack> encodeList(List<ItemStack> list) {
        ArrayList<ItemStack> out = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = i < list.size() ? list.get(i) : ItemStack.EMPTY;
            out.add(s == null ? ItemStack.EMPTY : s);
        }
        return out;
    }

    private static boolean isAllEmpty(List<ItemStack> list) {
        if (list == null || list.isEmpty()) return true;
        for (ItemStack s : list) {
            if (s != null && !s.isEmpty()) return false;
        }
        return true;
    }

    public static BankStorageState get(ServerWorld anyWorld) {
        ServerWorld overworld = anyWorld.getServer().getOverworld();
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    /**
     * Основной метод: получаем список по UUID.
     * Если UUID не найден, но есть записи по имени — мигрируем (одноразово) name -> uuid.
     */
    public DefaultedList<ItemStack> getOrCreate(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        String name = player.getName().getString();

        DefaultedList<ItemStack> cur = byUuid.get(uuid);
        if (cur != null) return ensureSize(cur, uuid);

        // fallback: если UUID поменялся, но имя то же — переносим предметы
        if (name != null && !name.isBlank()) {
            DefaultedList<ItemStack> fromName = byName.remove(name);
            if (fromName != null) {
                DefaultedList<ItemStack> fixed = ensureSize(fromName, uuid);
                byUuid.put(uuid, fixed);
                markDirty();
                return fixed;
            }
        }

        DefaultedList<ItemStack> created = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        byUuid.put(uuid, created);

        // на будущее: чтобы при “прыгающем UUID” было что мигрировать
        if (name != null && !name.isBlank()) byName.putIfAbsent(name, created);

        markDirty();
        return created;
    }

    private DefaultedList<ItemStack> ensureSize(DefaultedList<ItemStack> list, UUID uuid) {
        if (list.size() == SIZE) return list;
        DefaultedList<ItemStack> fixed = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(SIZE, list.size()); i++) fixed.set(i, list.get(i));
        byUuid.put(uuid, fixed);
        markDirty();
        return fixed;
    }

    public void markDirtyAndCleanup(ServerPlayerEntity player) {
        markDirty();

        UUID uuid = player.getUuid();
        String name = player.getName().getString();

        DefaultedList<ItemStack> list = byUuid.get(uuid);
        if (list != null && isAllEmpty(list)) byUuid.remove(uuid);

        if (name != null && !name.isBlank()) {
            DefaultedList<ItemStack> ln = byName.get(name);
            if (ln != null && isAllEmpty(ln)) byName.remove(name);
        }
    }
}