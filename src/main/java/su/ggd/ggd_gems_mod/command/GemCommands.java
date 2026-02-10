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
import su.ggd.ggd_gems_mod.config.GemsConfig;
import su.ggd.ggd_gems_mod.config.GemsConfigManager;
import su.ggd.ggd_gems_mod.gem.GemData;
import su.ggd.ggd_gems_mod.registry.ModItems;
import su.ggd.ggd_gems_mod.npc.GemTraderNpc;
import com.mojang.brigadier.arguments.StringArgumentType;



import java.util.Locale;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import su.ggd.ggd_gems_mod.net.GemsConfigSyncServer;
import su.ggd.ggd_gems_mod.config.NpcTradersConfigManager;


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
                        .requires(src -> {
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) return false;

                            return src.getServer()
                                    .getPlayerManager()
                                    .isOperator(player.getPlayerConfigEntry());
                        })
                        .then(literal("spawn")
                                .then(argument("id", StringArgumentType.word())
                                        .executes(ctx -> spawnNpc(ctx.getSource(), StringArgumentType.getString(ctx, "id"))))
                                .executes(ctx -> spawnNpc(ctx.getSource(), "gems")))
                        .then(literal("remove")
                                .executes(ctx -> removeNpc(ctx.getSource()))))
                .then(literal("reload")
                        .requires(src -> {
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) return false;

                            return src.getServer()
                                    .getPlayerManager()
                                    .isOperator(player.getPlayerConfigEntry());
                        })
                        .executes(ctx -> reload(ctx.getSource())))

        );
    }

    private static int give(ServerCommandSource src, String type, int levelInput) {
        GemsConfig cfg = GemsConfigManager.get();
        if (cfg == null) {
            src.sendError(Text.literal("Конфиг не загружен."));
            return 0;
        }

        String id = type.toLowerCase(Locale.ROOT);

        if (cfg.byType == null || !cfg.byType.containsKey(id)) {
            src.sendError(Text.literal("Неизвестный тип самоцвета: " + id));
            return 0;
        }

        int level = Math.max(1, levelInput);
        if (cfg.maxLevel > 0) level = Math.min(level, cfg.maxLevel);

        var player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("Команда доступна только игроку."));
            return 0;
        }

        // Создаём самоцвет
        ItemStack stack = new ItemStack(ModItems.GEM);
        GemData.set(stack, id, level);

        // ВАЖНО: имя берём из конфига (иначе будет название из lang)
        GemsConfig.GemDef def = cfg.byType.get(id);
        if (def != null && def.name != null && !def.name.isBlank()) {
            stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(def.name));
        } else {
            // на всякий случай убираем кастомное имя, чтобы не "залипало"
            stack.remove(DataComponentTypes.CUSTOM_NAME);
        }

        player.giveItemStack(stack);

        final int finalLevel = level;
        src.sendFeedback(() -> Text.literal("Выдан самоцвет: " + id + " lvl " + finalLevel), false);
        return 1;
    }

    private static int spawnNpc(ServerCommandSource src, String id) {
        var player = src.getPlayer();
        if (player == null) {
            src.sendError(Text.literal("Команда доступна только игроку."));
            return 0;
        }

        var entity = GemTraderNpc.spawnForPlayer(player, id);
        if (entity == null) {
            src.sendError(Text.literal("Не удалось создать NPC. Проверь npc_traders.json и id=" + id));
            return 0;
        }

        src.sendFeedback(() -> Text.literal("NPC создан: " + id), false);
        return 1;
    }

    private static int removeNpc(ServerCommandSource src) {
        var player = src.getPlayer();
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
        GemsConfigManager.loadOrCreateDefault();
        NpcTradersConfigManager.loadOrCreateDefault();

        GemsConfigSyncServer.broadcast(src.getServer());

        src.sendFeedback(() -> Text.literal("Конфиги перезагружены (server) и отправлены клиентам."), false);
        return 1;
    }


}
