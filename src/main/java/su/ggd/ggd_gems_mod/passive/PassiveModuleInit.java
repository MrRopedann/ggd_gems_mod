package su.ggd.ggd_gems_mod.passive;

import su.ggd.ggd_gems_mod.util.InitOnce;

public final class PassiveModuleInit {
    private PassiveModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("PassiveModuleInit")) return;

        // ВАЖНО: это серверные события/тики, должны быть зарегистрированы ровно один раз.
        PassiveSkillHooks.init();
    }
}
