package su.ggd.ggd_gems_mod.quests.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class QuestNet {
    private QuestNet() {}

    // S2C: quest definitions (all quests aggregated json)
    public static final CustomPayload.Id<SyncQuestDefsPayload> SYNC_QUEST_DEFS_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "sync_quest_defs"));

    public static final PacketCodec<RegistryByteBuf, SyncQuestDefsPayload> SYNC_QUEST_DEFS_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SyncQuestDefsPayload::hash,
                    PacketCodecs.STRING, SyncQuestDefsPayload::json,
                    SyncQuestDefsPayload::new
            );

    public record SyncQuestDefsPayload(String hash, String json) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return QuestNet.SYNC_QUEST_DEFS_ID; }
    }

    public static void sendDefs(ServerPlayerEntity player, String hash, String json) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new SyncQuestDefsPayload(hash, json));
    }
}
