package su.ggd.ggd_gems_mod.init.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import su.ggd.ggd_gems_mod.net.DamageIndicatorClient;
import su.ggd.ggd_gems_mod.net.DamageNet;
import su.ggd.ggd_gems_mod.net.GemsConfigSyncClient;
import su.ggd.ggd_gems_mod.net.MobLevelNet;
import su.ggd.ggd_gems_mod.net.MobLevelSyncClient;
import su.ggd.ggd_gems_mod.net.ModNet;
import su.ggd.ggd_gems_mod.net.PassiveSkillsSyncClient;
import su.ggd.ggd_gems_mod.net.QuestStateClientHandlers;
import su.ggd.ggd_gems_mod.net.QuestsSyncClient;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;
import su.ggd.ggd_gems_mod.quests.net.QuestNet;
import su.ggd.ggd_gems_mod.quests.net.QuestStateNet;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientNetInit {
    private ClientNetInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientNetInit")) return;

        // S2C receivers registered in one place.
        ClientPlayNetworking.registerGlobalReceiver(ModNet.SYNC_GEMS_CONFIG_ID, GemsConfigSyncClient::handle);

        ClientPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.SYNC_PASSIVES_CONFIG_ID, PassiveSkillsSyncClient::handleConfig);
        ClientPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.SYNC_PLAYER_PASSIVES_ID, PassiveSkillsSyncClient::handlePlayerLevels);
        ClientPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.UPGRADE_RESULT_ID, PassiveSkillsSyncClient::handleUpgradeResult);

        ClientPlayNetworking.registerGlobalReceiver(QuestNet.SYNC_QUEST_DEFS_ID, QuestsSyncClient::handleDefs);
        ClientPlayNetworking.registerGlobalReceiver(QuestStateNet.SYNC_PLAYER_STATE_ID, QuestStateClientHandlers::handleSyncPlayerState);

        ClientPlayNetworking.registerGlobalReceiver(MobLevelNet.MOB_LEVEL_ID, MobLevelSyncClient::handle);
        ClientPlayNetworking.registerGlobalReceiver(DamageNet.DAMAGE_INDICATOR_ID, DamageIndicatorClient::handle);
    }
}
