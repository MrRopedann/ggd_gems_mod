package su.ggd.ggd_gems_mod.quests.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class QuestStateNet {
    private QuestStateNet() {}

    // S2C: player quest state json
    public static final CustomPayload.Id<SyncPlayerQuestStatePayload> SYNC_PLAYER_STATE_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "sync_player_quest_state"));

    public static final PacketCodec<RegistryByteBuf, SyncPlayerQuestStatePayload> SYNC_PLAYER_STATE_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SyncPlayerQuestStatePayload::json,
                    SyncPlayerQuestStatePayload::new
            );

    public record SyncPlayerQuestStatePayload(String json) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return SYNC_PLAYER_STATE_ID; }
    }

    // C2S: start quest
    public static final CustomPayload.Id<C2SStartQuestPayload> C2S_START_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "c2s_quests_start"));
    public static final PacketCodec<RegistryByteBuf, C2SStartQuestPayload> C2S_START_CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, C2SStartQuestPayload::questId, C2SStartQuestPayload::new);
    public record C2SStartQuestPayload(String questId) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return C2S_START_ID; }
    }

    // C2S: abandon quest
    public static final CustomPayload.Id<C2SAbandonQuestPayload> C2S_ABANDON_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "c2s_quests_abandon"));
    public static final PacketCodec<RegistryByteBuf, C2SAbandonQuestPayload> C2S_ABANDON_CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, C2SAbandonQuestPayload::questId, C2SAbandonQuestPayload::new);
    public record C2SAbandonQuestPayload(String questId) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return C2S_ABANDON_ID; }
    }

    // C2S: track quest (questId or "none")
    public static final CustomPayload.Id<C2STrackQuestPayload> C2S_TRACK_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "c2s_quests_track"));
    public static final PacketCodec<RegistryByteBuf, C2STrackQuestPayload> C2S_TRACK_CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, C2STrackQuestPayload::questIdOrNone, C2STrackQuestPayload::new);
    public record C2STrackQuestPayload(String questIdOrNone) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return C2S_TRACK_ID; }
    }

    public static void sendPlayerState(ServerPlayerEntity player, String json) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new SyncPlayerQuestStatePayload(json));
    }
}
