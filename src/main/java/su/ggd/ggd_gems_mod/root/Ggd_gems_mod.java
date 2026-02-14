package su.ggd.ggd_gems_mod.root;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import su.ggd.ggd_gems_mod.command.GemCommands;
import su.ggd.ggd_gems_mod.command.StatsAllCommand;
import su.ggd.ggd_gems_mod.command.StatsCommand;
import su.ggd.ggd_gems_mod.command.WeaponStatsCommand;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.config.NpcTradersConfigManager;
import su.ggd.ggd_gems_mod.config.EconomyConfigManager;
import su.ggd.ggd_gems_mod.currency.PiastreDrops;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortNet;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortServer;
import su.ggd.ggd_gems_mod.net.GemsConfigSyncServer;
import su.ggd.ggd_gems_mod.net.ModNet;
import su.ggd.ggd_gems_mod.net.DamageNet;
import su.ggd.ggd_gems_mod.npc.NpcDamageBlocker;
import su.ggd.ggd_gems_mod.passive.effects.*;
import su.ggd.ggd_gems_mod.registry.ModDataComponents;
import su.ggd.ggd_gems_mod.registry.ModItemGroups;
import su.ggd.ggd_gems_mod.registry.ModItems;

import su.ggd.ggd_gems_mod.passive.PassiveSkillHooks;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsSyncServer;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsUpgradeServer;

import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffects;
import su.ggd.ggd_gems_mod.mob.MobLevels;

public class Ggd_gems_mod implements ModInitializer {

    public static final String MOD_ID = "ggd_gems_mod";

    @Override
    public void onInitialize() {
        // Damage indicator
        PayloadTypeRegistry.playS2C().register(DamageNet.DAMAGE_INDICATOR_ID, DamageNet.DAMAGE_INDICATOR_CODEC);

        // Gems config sync
        PayloadTypeRegistry.playS2C().register(ModNet.SYNC_GEMS_CONFIG_ID, ModNet.SYNC_GEMS_CONFIG_CODEC);

        // Passive skills net:
        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.SYNC_PASSIVES_CONFIG_ID, PassiveSkillsNet.SYNC_PASSIVES_CONFIG_CODEC);
        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.SYNC_PLAYER_PASSIVES_ID, PassiveSkillsNet.SYNC_PLAYER_PASSIVES_CODEC);
        PayloadTypeRegistry.playC2S().register(PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_ID, PassiveSkillsNet.UPGRADE_PASSIVE_SKILL_CODEC);

        PayloadTypeRegistry.playS2C().register(PassiveSkillsNet.UPGRADE_RESULT_ID, PassiveSkillsNet.UPGRADE_RESULT_CODEC);
        // Inventory sort (client -> server)
        PayloadTypeRegistry.playC2S().register(InventorySortNet.SORT_REQUEST_ID, InventorySortNet.SORT_REQUEST_CODEC);


        PassiveSkillEffects.register(new OakRootsEffect());
        PassiveSkillEffects.register(new LuckyMinerEffect());
        PassiveSkillEffects.register(new DiligentStudentEffect());
        PassiveSkillEffects.register(new MeleeDamageEffect());
        PassiveSkillEffects.register(new RangedDamageEffect());
        PassiveSkillEffects.register(new MoveSpeedEffect());
        PassiveSkillEffects.register(new BonusHpEffect());
        PassiveSkillEffects.register(new BowTrajectoryEffect());
        PassiveSkillEffects.register(new LightHandEffect());

        // 1) Реестры/компоненты
        ModDataComponents.initialize();

        // 2) Конфиги (сервер-истина)
        GemsConfigManager.loadOrCreateDefault();
        NpcTradersConfigManager.loadOrCreateDefault();
        EconomyConfigManager.loadOrCreateDefault();

        // Пассивки
        PassiveSkillsConfigManager.loadOrCreateDefault();

        // 3) Синк на клиент при входе
        GemsConfigSyncServer.init();
        PassiveSkillsSyncServer.init();

        // Inventory sort receiver
        InventorySortServer.init();


        // 4) Контент
        ModItems.initialize();
        ModItemGroups.initialize();
        NpcDamageBlocker.init();
        PiastreDrops.register();

        // 4.5) Уровни мобов (сервер)
        MobLevels.init();

        // 5) Пассивки — hooks + серверный обработчик апгрейда
        PassiveSkillHooks.init();
        PassiveSkillsUpgradeServer.init();

        // 6) Команды
        CommandRegistrationCallback.EVENT.register(GemCommands::register);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            StatsAllCommand.register(dispatcher);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            StatsCommand.register(dispatcher);
            WeaponStatsCommand.register(dispatcher);
        });
    }
}
