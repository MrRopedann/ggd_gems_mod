package su.ggd.ggd_gems_mod.quests.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.*;

public final class QuestsState extends PersistentState {

    // players saved as: { "players": { "<uuid>": { ...playerData... } } }
    private static final Codec<Map<String, PlayerQuestData>> PLAYERS_CODEC =
            Codec.unboundedMap(Codec.STRING, PlayerQuestData.CODEC);

    public static final Codec<QuestsState> CODEC = RecordCodecBuilder.create(i -> i.group(
            PLAYERS_CODEC.optionalFieldOf("players", Map.<String, PlayerQuestData>of())
                    .forGetter(QuestsState::encodePlayers)
    ).apply(i, QuestsState::newFromCodec));

    public static final PersistentStateType<QuestsState> TYPE =
            new PersistentStateType<>(
                    "ggd_gems_mod_quests",
                    QuestsState::new,
                    CODEC,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE
            );

    // runtime storage
    private final Map<UUID, PlayerQuestData> players = new HashMap<>();

    public QuestsState() {}

    private static QuestsState newFromCodec(Map<String, PlayerQuestData> playersById) {
        QuestsState st = new QuestsState();
        if (playersById == null || playersById.isEmpty()) return st;

        for (var e : playersById.entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                PlayerQuestData d = e.getValue();
                if (uuid == null || d == null) continue;
                st.players.put(uuid, d);
            } catch (Exception ignored) {}
        }
        return st;
    }

    private Map<String, PlayerQuestData> encodePlayers() {
        HashMap<String, PlayerQuestData> out = new HashMap<>();
        for (var e : players.entrySet()) {
            UUID uuid = e.getKey();
            PlayerQuestData d = e.getValue();
            if (uuid == null || d == null) continue;
            out.put(uuid.toString(), d);
        }
        return out;
    }

    public static QuestsState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    public PlayerQuestData getOrCreate(UUID uuid) {
        return players.computeIfAbsent(uuid, u -> new PlayerQuestData());
    }

    // --------- nested data ----------

    public static final class PlayerQuestData {
        public static final Codec<PlayerQuestData> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(Codec.STRING, ActiveQuestProgress.CODEC)
                        .optionalFieldOf("active", Map.<String, ActiveQuestProgress>of())
                        .forGetter((PlayerQuestData d) -> d.activeQuests),

                Codec.STRING.listOf()
                        .optionalFieldOf("completed", List.<String>of())
                        .forGetter((PlayerQuestData d) -> new ArrayList<>(d.completedQuests)),

                Codec.STRING.listOf()
                        .optionalFieldOf("unlocked", List.<String>of())
                        .forGetter((PlayerQuestData d) -> new ArrayList<>(d.unlockedQuests)),

                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                        .optionalFieldOf("flags", Map.<String, String>of())
                        .forGetter((PlayerQuestData d) -> d.flags),

                Codec.STRING.optionalFieldOf("tracked")
                        .forGetter((PlayerQuestData d) -> Optional.ofNullable(d.trackedQuestId))
        ).apply(i, (active, completed, unlocked, flags, tracked) -> {
            PlayerQuestData d = new PlayerQuestData();
            d.activeQuests.putAll(active);
            d.completedQuests.addAll(completed);
            d.unlockedQuests.addAll(unlocked);
            d.flags.putAll(flags);
            d.trackedQuestId = tracked.orElse(null);
            return d;
        }));

        public final Map<String, ActiveQuestProgress> activeQuests = new HashMap<>();
        public final Set<String> completedQuests = new HashSet<>();
        public final Set<String> unlockedQuests = new HashSet<>();

        // V1: flags as String->String for simple codec
        public final Map<String, String> flags = new HashMap<>();

        public String trackedQuestId = null;
    }

    public static final class ActiveQuestProgress {
        public static final Codec<ActiveQuestProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("questId")
                        .forGetter((ActiveQuestProgress p) -> p.questId),

                Codec.STRING.fieldOf("stageId")
                        .forGetter((ActiveQuestProgress p) -> p.currentStageId),

                Codec.unboundedMap(Codec.STRING, Codec.INT)
                        .optionalFieldOf("objectives", Map.<String, Integer>of())
                        .forGetter((ActiveQuestProgress p) -> p.objectives),

                Codec.LONG.optionalFieldOf("stageStartedAt")
                        .forGetter((ActiveQuestProgress p) -> Optional.of(p.stageStartedAtTime)),

                Codec.LONG.optionalFieldOf("lastUpdateAt")
                        .forGetter((ActiveQuestProgress p) -> Optional.of(p.lastUpdateTime))
        ).apply(i, (questId, stageId, objectives, stageStartedAt, lastUpdateAt) -> {
            ActiveQuestProgress p = new ActiveQuestProgress(questId, stageId);
            p.objectives.putAll(objectives);
            p.stageStartedAtTime = stageStartedAt.orElse(0L);
            p.lastUpdateTime = lastUpdateAt.orElse(0L);
            return p;
        }));

        public final String questId;
        public String currentStageId;

        public final Map<String, Integer> objectives = new HashMap<>();

        public long stageStartedAtTime;
        public long lastUpdateTime;

        public ActiveQuestProgress(String questId, String stageId) {
            this.questId = questId;
            this.currentStageId = stageId;
        }
    }
}
