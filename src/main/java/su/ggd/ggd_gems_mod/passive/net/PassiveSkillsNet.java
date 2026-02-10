package su.ggd.ggd_gems_mod.passive.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.root.Ggd_gems_mod;

public final class PassiveSkillsNet {
    private PassiveSkillsNet() {}

    // S2C: конфиг пассивок
    public static final CustomPayload.Id<SyncPassiveSkillsConfigPayload> SYNC_PASSIVES_CONFIG_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "sync_passive_skills_config"));

    public static final PacketCodec<RegistryByteBuf, SyncPassiveSkillsConfigPayload> SYNC_PASSIVES_CONFIG_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SyncPassiveSkillsConfigPayload::hash,
                    PacketCodecs.STRING, SyncPassiveSkillsConfigPayload::json,
                    SyncPassiveSkillsConfigPayload::new
            );

    public record SyncPassiveSkillsConfigPayload(String hash, String json) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return PassiveSkillsNet.SYNC_PASSIVES_CONFIG_ID; }
    }

    // S2C: уровни навыков игрока (json map)
    public static final CustomPayload.Id<SyncPlayerPassiveSkillsPayload> SYNC_PLAYER_PASSIVES_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "sync_player_passives"));

    public static final PacketCodec<RegistryByteBuf, SyncPlayerPassiveSkillsPayload> SYNC_PLAYER_PASSIVES_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, SyncPlayerPassiveSkillsPayload::json,
                    SyncPlayerPassiveSkillsPayload::new
            );

    public record SyncPlayerPassiveSkillsPayload(String json) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return PassiveSkillsNet.SYNC_PLAYER_PASSIVES_ID; }
    }

    // C2S: запрос на улучшение
    public static final CustomPayload.Id<UpgradePassiveSkillPayload> UPGRADE_PASSIVE_SKILL_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "upgrade_passive_skill"));

    public static final PacketCodec<RegistryByteBuf, UpgradePassiveSkillPayload> UPGRADE_PASSIVE_SKILL_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, UpgradePassiveSkillPayload::skillId,
                    UpgradePassiveSkillPayload::new
            );

    public record UpgradePassiveSkillPayload(String skillId) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_ID; }
    }

    // S2C: результат улучшения (в окно)
    public static final CustomPayload.Id<UpgradeResultPayload> UPGRADE_RESULT_ID =
            new CustomPayload.Id<>(Identifier.of(Ggd_gems_mod.MOD_ID, "passive_upgrade_result"));

    public static final PacketCodec<RegistryByteBuf, UpgradeResultPayload> UPGRADE_RESULT_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BYTE, p -> (byte) (p.ok() ? 1 : 0),
                    PacketCodecs.STRING, UpgradeResultPayload::message,
                    (b, msg) -> new UpgradeResultPayload(b != 0, msg)
            );

    public record UpgradeResultPayload(boolean ok, String message) implements CustomPayload {
        @Override public Id<? extends CustomPayload> getId() { return PassiveSkillsNet.UPGRADE_RESULT_ID; }
    }

    // server helper
    public static void sendUpgradeResult(ServerPlayerEntity player, boolean ok, String msg) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new UpgradeResultPayload(ok, msg));
    }
}
