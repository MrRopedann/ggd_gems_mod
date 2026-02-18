package su.ggd.ggd_gems_mod.quests.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.quests.service.QuestService;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class QuestsSyncServer {
    private QuestsSyncServer() {}

    public static void sendDefsTo(ServerPlayerEntity player) {
        String json = QuestsConfigManager.toJsonString();
        if (json == null) json = "{}";

        String hash = QuestsConfigManager.sha256(json);
        QuestNet.sendDefs(player, hash, json);
    }
}
