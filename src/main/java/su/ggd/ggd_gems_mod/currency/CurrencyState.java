package su.ggd.ggd_gems_mod.currency.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CurrencyState extends PersistentState {

    private final Map<UUID, Long> balances = new HashMap<>();

    private static final Codec<Map<String, Long>> PLAYERS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.LONG);

    public static final Codec<CurrencyState> CODEC = RecordCodecBuilder.create(i -> i.group(
            PLAYERS_CODEC.optionalFieldOf("players", Map.of()).forGetter(CurrencyState::encodePlayers)
    ).apply(i, CurrencyState::newFromCodec));

    public static final PersistentStateType<CurrencyState> TYPE =
            new PersistentStateType<>(
                    "ggd_gems_mod_currency",
                    CurrencyState::new,
                    CODEC,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE
            );

    public CurrencyState() {}

    private static CurrencyState newFromCodec(Map<String, Long> players) {
        CurrencyState st = new CurrencyState();
        if (players == null || players.isEmpty()) return st;

        for (var e : players.entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                Long v = e.getValue();
                if (uuid == null || v == null) continue;
                long bal = Math.max(0L, v);
                if (bal > 0L) st.balances.put(uuid, bal);
            } catch (Exception ignored) {}
        }
        return st;
    }

    private Map<String, Long> encodePlayers() {
        HashMap<String, Long> out = new HashMap<>();
        for (var e : balances.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            long v = Math.max(0L, e.getValue());
            if (v <= 0L) continue;
            out.put(e.getKey().toString(), v);
        }
        return out;
    }

    public static CurrencyState get(ServerWorld anyWorld) {
        ServerWorld overworld = anyWorld.getServer().getOverworld();
        return overworld.getPersistentStateManager().getOrCreate(TYPE);
    }

    public long getBalance(UUID playerId) {
        if (playerId == null) return 0L;
        return Math.max(0L, balances.getOrDefault(playerId, 0L));
    }

    public void setBalance(UUID playerId, long value) {
        if (playerId == null) return;
        long v = Math.max(0L, value);
        if (v <= 0L) balances.remove(playerId);
        else balances.put(playerId, v);
        markDirty();
    }
}
