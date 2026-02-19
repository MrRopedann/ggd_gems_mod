package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import su.ggd.ggd_gems_mod.currency.CurrencySyncServer;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsSyncServer;
import su.ggd.ggd_gems_mod.quests.net.QuestsSyncServer;
import su.ggd.ggd_gems_mod.util.InitOnce;
import su.ggd.ggd_gems_mod.currency.CurrencyService;


public final class JoinSyncInit {
    private JoinSyncInit() {}

    public static void init() {
        if (!InitOnce.markDone("JoinSyncInit")) return;

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;

            // конфиги
            GemsConfigSyncServer.sendTo(p);
            PassiveSkillsSyncServer.sendConfigTo(p);
            QuestsSyncServer.sendDefsTo(p);

            CurrencySyncServer.sendCurrentTo(p);

            // состояния
            PassiveSkillsSyncServer.sendPlayerSkillsTo(p);
            su.ggd.ggd_gems_mod.quests.service.QuestService.sync(p);

            CurrencyService.syncCurrent(p);

        });
    }
}
