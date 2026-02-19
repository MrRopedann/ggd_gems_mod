package su.ggd.ggd_gems_mod.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import su.ggd.ggd_gems_mod.currency.net.BankNet;
import su.ggd.ggd_gems_mod.currency.net.CurrencyNet;
import su.ggd.ggd_gems_mod.currency.server.BankServerHandlers;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortNet;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortServer;
import su.ggd.ggd_gems_mod.npc.net.NpcTraderNet;
import su.ggd.ggd_gems_mod.npc.server.NpcTraderServerHandlers;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsUpgradeServer;
import su.ggd.ggd_gems_mod.quests.net.QuestNet;
import su.ggd.ggd_gems_mod.quests.net.QuestStateNet;
import su.ggd.ggd_gems_mod.quests.net.QuestStateServerHandlers;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class NetworkingModuleInit {
    private NetworkingModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("NetworkingModuleInit")) return;

        registerPayloads();

        // C2S receivers registered in one place.
        ServerPlayNetworking.registerGlobalReceiver(InventorySortNet.SORT_REQUEST_ID, InventorySortServer::handleSortRequest);
        ServerPlayNetworking.registerGlobalReceiver(PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_ID, PassiveSkillsUpgradeServer::handleUpgrade);

        ServerPlayNetworking.registerGlobalReceiver(QuestStateNet.C2S_START_ID, QuestStateServerHandlers::handleStart);
        ServerPlayNetworking.registerGlobalReceiver(QuestStateNet.C2S_ABANDON_ID, QuestStateServerHandlers::handleAbandon);
        ServerPlayNetworking.registerGlobalReceiver(QuestStateNet.C2S_TRACK_ID, QuestStateServerHandlers::handleTrack);

        ServerPlayNetworking.registerGlobalReceiver(BankNet.WITHDRAW_ID, BankServerHandlers::handleWithdraw);
        ServerPlayNetworking.registerGlobalReceiver(BankNet.DEPOSIT_ALL_ID, BankServerHandlers::handleDepositAll);

        ServerPlayNetworking.registerGlobalReceiver(NpcTraderNet.BUY_ID, NpcTraderServerHandlers::handleBuy);

        JoinSyncInit.init();
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(DamageNet.DAMAGE_INDICATOR_ID, DamageNet.DAMAGE_INDICATOR_CODEC);
        PayloadTypeRegistry.playS2C().register(ModNet.SYNC_GEMS_CONFIG_ID, ModNet.SYNC_GEMS_CONFIG_CODEC);

        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.SYNC_PASSIVES_CONFIG_ID, PassiveSkillsNet.SYNC_PASSIVES_CONFIG_CODEC);
        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.SYNC_PLAYER_PASSIVES_ID, PassiveSkillsNet.SYNC_PLAYER_PASSIVES_CODEC);
        PayloadTypeRegistry.playC2S().register(PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_ID, PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_CODEC);
        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.UPGRADE_RESULT_ID, PassiveSkillsNet.UPGRADE_RESULT_CODEC);

        PayloadTypeRegistry.playC2S().register(InventorySortNet.SORT_REQUEST_ID, InventorySortNet.SORT_REQUEST_CODEC);

        PayloadTypeRegistry.playS2C().register(MobLevelNet.MOB_LEVEL_ID, MobLevelNet.MOB_LEVEL_CODEC);

        PayloadTypeRegistry.playS2C().register(QuestNet.SYNC_QUEST_DEFS_ID, QuestNet.SYNC_QUEST_DEFS_CODEC);

        PayloadTypeRegistry.playS2C().register(QuestStateNet.SYNC_PLAYER_STATE_ID, QuestStateNet.SYNC_PLAYER_STATE_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestStateNet.C2S_START_ID, QuestStateNet.C2S_START_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestStateNet.C2S_ABANDON_ID, QuestStateNet.C2S_ABANDON_CODEC);
        PayloadTypeRegistry.playC2S().register(QuestStateNet.C2S_TRACK_ID, QuestStateNet.C2S_TRACK_CODEC);

        PayloadTypeRegistry.playS2C().register(CurrencyNet.SYNC_BALANCE_ID, CurrencyNet.SYNC_BALANCE_CODEC);

        PayloadTypeRegistry.playC2S().register(BankNet.WITHDRAW_ID, BankNet.WITHDRAW_CODEC);
        PayloadTypeRegistry.playC2S().register(BankNet.DEPOSIT_ALL_ID, BankNet.DEPOSIT_ALL_CODEC);

        PayloadTypeRegistry.playS2C().register(NpcTraderNet.OPEN_TRADER_ID, NpcTraderNet.OPEN_TRADER_CODEC);
        PayloadTypeRegistry.playC2S().register(NpcTraderNet.BUY_ID, NpcTraderNet.BUY_CODEC);
    }
}
