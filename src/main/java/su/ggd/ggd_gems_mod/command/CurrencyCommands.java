package su.ggd.ggd_gems_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.currency.CurrencyService;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class CurrencyCommands {
    private CurrencyCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> d, CommandRegistryAccess access) {
        d.register(literal("currency")
                // В твоём ServerCommandSource нет hasPermissionLevel(int)
                .requires(src -> src.getPermissions() != null)

                .then(literal("get")
                        .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player");
                                    long bal = CurrencyService.getBalance(p);
                                    ctx.getSource().sendFeedback(() -> Text.literal(p.getName().getString() + ": " + bal), false);
                                    return 1;
                                })))

                .then(literal("set")
                        .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                                .then(argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            CurrencyService.setBalance(p, amount);
                                            ctx.getSource().sendFeedback(() -> Text.literal("OK"), false);
                                            return 1;
                                        }))))

                .then(literal("add")
                        .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                                .then(argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            CurrencyService.add(p, amount);
                                            ctx.getSource().sendFeedback(() -> Text.literal("OK"), false);
                                            return 1;
                                        }))))
        );
    }
}
