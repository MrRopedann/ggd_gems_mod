package su.ggd.ggd_gems_mod.init;

import su.ggd.ggd_gems_mod.command.CommandsModuleInit;
import su.ggd.ggd_gems_mod.config.ConfigModuleInit;
import su.ggd.ggd_gems_mod.currency.EconomyModuleInit;
import su.ggd.ggd_gems_mod.gem.GemsModuleInit;
import su.ggd.ggd_gems_mod.inventory.sort.InventorySortModuleInit;
import su.ggd.ggd_gems_mod.mob.MobModuleInit;
import su.ggd.ggd_gems_mod.net.NetworkingModuleInit;
import su.ggd.ggd_gems_mod.npc.NpcModuleInit;
import su.ggd.ggd_gems_mod.passive.PassiveModuleInit;
import su.ggd.ggd_gems_mod.quests.QuestsModuleInit;
import su.ggd.ggd_gems_mod.registry.ContentModuleInit;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ServerInitPlan {
    private ServerInitPlan() {}

    public static void init() {
        if (!InitOnce.markDone("ServerInitPlan")) return;

        NetworkingModuleInit.init();
        ContentModuleInit.init();
        ConfigModuleInit.init();

        GemsModuleInit.init();
        NpcModuleInit.init();
        MobModuleInit.init();
        PassiveModuleInit.init();
        QuestsModuleInit.init();
        InventorySortModuleInit.init();
        EconomyModuleInit.init();

        CommandsModuleInit.init();
    }
}
