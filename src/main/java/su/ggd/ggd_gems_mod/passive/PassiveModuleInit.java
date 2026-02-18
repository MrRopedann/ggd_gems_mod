package su.ggd.ggd_gems_mod.passive;

import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsUpgradeServer;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class PassiveModuleInit {
    private PassiveModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("PassiveModuleInit")) return;

        PassiveSkillHooks.init();
        PassiveSkillsUpgradeServer.init();
    }
}
