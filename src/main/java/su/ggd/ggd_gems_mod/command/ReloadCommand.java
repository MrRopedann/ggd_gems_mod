package su.ggd.ggd_gems_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.config.ConfigRegistry;

import static net.minecraft.server.command.CommandManager.literal;

public final class ReloadCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("ggd")
                .then(literal("reload")
                        .executes(ctx -> {
                            long t0 = System.nanoTime();
                            ConfigRegistry.loadAll();
                            long ms = (System.nanoTime() - t0) / 1_000_000L;
                            ctx.getSource().sendFeedback(() -> Text.literal("GGD: конфиги перезагружены за " + ms + " ms."), true);
                            return 1;
                        })
                )
        );

    }

    private ReloadCommand() {}
}
