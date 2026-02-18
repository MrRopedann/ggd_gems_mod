package su.ggd.ggd_gems_mod.quests.net;

import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;

public final class QuestsSyncServer {
    private QuestsSyncServer() {}

    public static void sendDefsTo(ServerPlayerEntity player) {
        String json = QuestsConfigManager.toJsonString();
        if (json == null) json = "{}";

        String hash = QuestsConfigManager.sha256(json);
        QuestNet.sendDefs(player, hash, json);
    }
}
