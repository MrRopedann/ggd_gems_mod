package su.ggd.ggd_gems_mod.quests.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.quests.service.QuestService;

public final class QuestsSyncServer {
    private QuestsSyncServer() {}

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            sendDefsTo(p);
            QuestService.sync(p); // NEW: state sync
        });
    }

    public static void sendDefsTo(ServerPlayerEntity player) {
        String json = QuestsConfigManager.toJsonString();
        String hash = QuestsConfigManager.sha256(json);
        QuestNet.sendDefs(player, hash, json);
    }
}
