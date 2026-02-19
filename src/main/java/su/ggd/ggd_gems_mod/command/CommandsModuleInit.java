package su.ggd.ggd_gems_mod.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import su.ggd.ggd_gems_mod.quests.command.QuestsCommands;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class CommandsModuleInit {
    private CommandsModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("CommandsModuleInit")) return;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // GemCommands требует 3 аргумента
            GemCommands.register(dispatcher, registryAccess, environment);

            // остальные команды в проекте
            StatsCommand.register(dispatcher);
            StatsAllCommand.register(dispatcher);
            WeaponStatsCommand.register(dispatcher);

            QuestsCommands.register(dispatcher);

            ReloadCommand.register(dispatcher);

            CurrencyCommands.register(dispatcher, registryAccess);

        });
    }
}
