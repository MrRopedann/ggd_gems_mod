package su.ggd.ggd_gems_mod.passive.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.*;

public final class PassiveSkillsState extends PersistentState {
    private static final Gson GSON = new GsonBuilder().create();

    // Внутреннее хранение: player -> (skillId -> level)
    private final Map<UUID, Map<String, Integer>> data = new HashMap<>();

    // ===== Codec serialization =====
    // сохраняем как: { "players": { "<uuid>": { "oak_roots": 3 } } }
    private static final Codec<Map<String, Map<String, Integer>>> PLAYERS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT));

    public static final Codec<PassiveSkillsState> CODEC = RecordCodecBuilder.create(i -> i.group(
            PLAYERS_CODEC.optionalFieldOf("players", Map.of()).forGetter(PassiveSkillsState::encodePlayers)
    ).apply(i, PassiveSkillsState::newFromCodec));

    public static final PersistentStateType<PassiveSkillsState> TYPE =
            new PersistentStateType<>(
                    "ggd_gems_mod_passive_skills",
                    PassiveSkillsState::new,
                    CODEC,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE
            );

    public PassiveSkillsState() {}

    private static PassiveSkillsState newFromCodec(Map<String, Map<String, Integer>> players) {
        PassiveSkillsState st = new PassiveSkillsState();
        if (players == null) return st;

        for (var pe : players.entrySet()) {
            try {
                UUID uuid = UUID.fromString(pe.getKey());
                Map<String, Integer> skills = pe.getValue();
                if (skills == null || skills.isEmpty()) continue;

                HashMap<String, Integer> norm = new HashMap<>();
                for (var se : skills.entrySet()) {
                    if (se.getKey() == null) continue;
                    String id = se.getKey().toLowerCase(Locale.ROOT);
                    int lv = (se.getValue() == null) ? 0 : se.getValue();
                    if (lv > 0) norm.put(id, lv);
                }
                if (!norm.isEmpty()) st.data.put(uuid, norm);
            } catch (Exception ignored) {}
        }
        return st;
    }

    private Map<String, Map<String, Integer>> encodePlayers() {
        HashMap<String, Map<String, Integer>> out = new HashMap<>();
        for (var e : data.entrySet()) {
            UUID uuid = e.getKey();
            Map<String, Integer> skills = e.getValue();
            if (uuid == null || skills == null || skills.isEmpty()) continue;
            out.put(uuid.toString(), new HashMap<>(skills));
        }
        return out;
    }

    // ===== Access =====

    public static PassiveSkillsState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public int getLevel(UUID playerId, String skillId) {
        if (playerId == null || skillId == null) return 0;
        Map<String, Integer> m = data.get(playerId);
        if (m == null) return 0;
        return Math.max(0, m.getOrDefault(skillId.toLowerCase(Locale.ROOT), 0));
    }

    public void setLevel(UUID playerId, String skillId, int level) {
        if (playerId == null || skillId == null) return;
        String id = skillId.toLowerCase(Locale.ROOT);
        int lv = Math.max(0, level);

        Map<String, Integer> m = data.computeIfAbsent(playerId, k -> new HashMap<>());
        if (lv <= 0) m.remove(id);
        else m.put(id, lv);

        markDirty();
    }

    public String toJsonFor(UUID playerId) {
        Map<String, Integer> m = data.get(playerId);
        if (m == null) m = Map.of();
        return GSON.toJson(m);
    }
}
