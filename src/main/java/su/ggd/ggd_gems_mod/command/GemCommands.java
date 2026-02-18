package su.ggd.ggd_gems_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.config.ConfigRegistry;
import su.ggd.ggd_gems_mod.gem.GemData;
import su.ggd.ggd_gems_mod.gem.GemRegistry;
import su.ggd.ggd_gems_mod.net.GemsConfigSyncServer;
import su.ggd.ggd_gems_mod.npc.GemTraderNpc;
import su.ggd.ggd_gems_mod.registry.ModItems;

import java.util.Locale;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class GemCommands {
    private GemCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                net.minecraft.command.CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {

        dispatcher.register(literal("gem")
                .then(literal("give")
                        .then(argument("type", StringArgumentType.word())
                                .then(argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> give(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "type"),
                                                IntegerArgumentType.getInteger(ctx, "level")
                                        )))
                                .executes(ctx -> give(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type"),
                                        1
                                )))
                )
                .then(literal("npc")
                        .requires(GemCommands::isOpPlayer)
                        .then(literal("spawn")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(ctx -> spawnNpc(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))
                                .executes(ctx -> spawnNpc(ctx.getSource(), "gems")))
                        .then(literal("remove")
                                .executes(ctx -> removeNpc(ctx.getSource()))))
                .then(literal("reload")
                        .requires(GemCommands::isOpPlayer)
                        .executes(ctx -> reload(ctx.getSource())))
        );
    }

    private static boolean isOpPlayer(ServerCommandSource src) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) return false;
        return src.getServer().getPlayerManager().isOperator(player.getPlayerConfigEntry());
    }

    private static int give(ServerCommandSource src, String type, int levelInput) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("Команда доступна только игроку."));
            return 0;
        }

        String id = (type == null ? "" : type).trim().toLowerCase(Locale.ROOT);
        if (id.isBlank()) {
            src.sendError(Text.literal("Пустой тип самоцвета."));
            return 0;
        }

        var def = GemRegistry.get(id);
        if (def == null) {
            src.sendError(Text.literal("Неизвестный тип самоцвета: " + id));
            return 0;
        }

        int max = (def.maxLevel > 0) ? def.maxLevel : 10;
        int level = Math.max(1, levelInput);
        level = Math.min(level, max);

        ItemStack stack = new ItemStack(ModItems.GEM);
        GemData.set(stack, id, level);

        // имя берём из def.name (или убираем CUSTOM_NAME, чтобы использовался lang "Самоцвет")
        if (def.name != null && !def.name.isBlank()) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(def.name));
        } else {
            stack.remove(DataComponentTypes.CUSTOM_NAME);
        }

        player.giveItemStack(stack);

        int finalLevel = level;
        src.sendFeedback(() -> Text.literal("Выдан самоцвет: " + id + " lvl " + finalLevel + "/" + max), false);
        return 1;
    }

    private static int spawnNpc(ServerCommandSource src, String id) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("Команда доступна только игроку."));
            return 0;
        }

        String traderId = (id == null ? "" : id).trim().toLowerCase(Locale.ROOT);
        if (traderId.isBlank()) traderId = "gems";

        var entity = GemTraderNpc.spawnForPlayer(player, traderId);
        if (entity == null) {
            src.sendError(Text.literal("Не удалось создать NPC. Проверь npc_traders.json и id=" + traderId));
            return 0;
        }

        String finalId = traderId;
        src.sendFeedback(() -> Text.literal("NPC создан: " + finalId), false);
        return 1;
    }

    private static int removeNpc(ServerCommandSource src) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("Команда доступна только игроку."));
            return 0;
        }

        var target = GemTraderNpc.getLookedAtTrader(player, 6.0);
        if (target == null) {
            src.sendError(Text.literal("Ты не смотришь на торговца (или он слишком далеко)."));
            return 0;
        }

        target.discard();
        src.sendFeedback(() -> Text.literal("Торговец удалён."), false);
        return 1;
    }

    private static int reload(ServerCommandSource src) {
        // единая система reload + post-reload hooks (в т.ч. GemRegistry.rebuild())
        ConfigRegistry.loadAll();

        // синк конфигов клиентам (если у тебя пакет sync содержит только gems — оставить)
        GemsConfigSyncServer.broadcast(src.getServer());

        src.sendFeedback(() -> Text.literal("Конфиги перезагружены и отправлены клиентам."), false);
        return 1;
    }
}
