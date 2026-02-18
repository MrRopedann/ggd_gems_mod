package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortNet;
import su.ggd.ggd_gems_mod.net.DamageNet;
import su.ggd.ggd_gems_mod.net.MobLevelNet;
import su.ggd.ggd_gems_mod.net.ModNet;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsSyncServer;
import su.ggd.ggd_gems_mod.quests.net.QuestNet;
import su.ggd.ggd_gems_mod.quests.net.QuestStateNet;
import su.ggd.ggd_gems_mod.quests.net.QuestStateServerHandlers;
import su.ggd.ggd_gems_mod.quests.net.QuestsSyncServer;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class NetworkingModuleInit {
    private NetworkingModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("NetworkingModuleInit")) return;

        // 1) Payload codecs (всегда до любых send/receive)
        registerPayloads();

        // 2) C2S handlers
        QuestStateServerHandlers.init();

        // 3) Sync on join
        GemsConfigSyncServer.init();
        PassiveSkillsSyncServer.init();
        QuestsSyncServer.init();

        // Inventory sort receiver — в InventorySortModuleInit
    }

    private static void registerPayloads() {
        // Damage indicator
        PayloadTypeRegistry.playS2C().register(DamageNet.DAMAGE_INDICATOR_ID, DamageNet.DAMAGE_INDICATOR_CODEC);

        // Gems config sync
        PayloadTypeRegistry.playS2C().register(ModNet.SYNC_GEMS_CONFIG_ID, ModNet.SYNC_GEMS_CONFIG_CODEC);

        // Passive skills
        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.SYNC_PASSIVES_CONFIG_ID, PassiveSkillsNet.SYNC_PASSIVES_CONFIG_CODEC);
        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.SYNC_PLAYER_PASSIVES_ID, PassiveSkillsNet.SYNC_PLAYER_PASSIVES_CODEC);
        PayloadTypeRegistry.playC2S().register(PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_ID, PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_CODEC);
        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.UPGRADE_RESULT_ID, PassiveSkillsNet.UPGRADE_RESULT_CODEC);

        // Inventory sort
        PayloadTypeRegistry.playC2S().register(InventorySortNet.SORT_REQUEST_ID, InventorySortNet.SORT_REQUEST_CODEC);

        // Mob level
        PayloadTypeRegistry.playS2C().register(MobLevelNet.MOB_LEVEL_ID, MobLevelNet.MOB_LEVEL_CODEC);

        // Quests defs sync
        PayloadTypeRegistry.playS2C().register(QuestNet.SYNC_QUEST_DEFS_ID, QuestNet.SYNC_QUEST_DEFS_CODEC);

        // Quests state sync + C2S
        PayloadTypeRegistry.playS2C().register(QuestStateNet.SYNC_PLAYER_STATE_ID, QuestStateNet.SYNC_PLAYER_STATE_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestStateNet.C2S_START_ID, QuestStateNet.C2S_START_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestStateNet.C2S_ABANDON_ID, QuestStateNet.C2S_ABANDON_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestStateNet.C2S_TRACK_ID, QuestStateNet.C2S_TRACK_CODEC);
    }
}
