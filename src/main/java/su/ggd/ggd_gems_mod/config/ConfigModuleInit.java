package su.ggd.ggd_gems_mod.config;

import su.ggd.ggd_gems_mod.gem.GemRegistry;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ConfigModuleInit {
    private ConfigModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("ConfigModuleInit")) return;

        ConfigRegistry.register(new RunnableReloadableConfig("gems", GemsConfigManager::loadOrCreateDefault));
        ConfigRegistry.register(new RunnableReloadableConfig("npc_traders", NpcTradersConfigManager::loadOrCreateDefault));
        ConfigRegistry.register(new RunnableReloadableConfig("economy", EconomyConfigManager::loadOrCreateDefault));
        ConfigRegistry.register(new RunnableReloadableConfig("quests", QuestsConfigManager::loadOrCreateDefault));
        ConfigRegistry.register(new RunnableReloadableConfig("passive_skills", PassiveSkillsConfigManager::loadOrCreateDefault));
        ConfigRegistry.register(new RunnableReloadableConfig("mob_rules", MobRulesConfigManager::loadOrCreateDefault));

        ConfigRegistry.freeze();

        ReloadHooks.register(GemRegistry::rebuild);
        ReloadHooks.freeze();

        ConfigRegistry.loadAll();
    }
}
