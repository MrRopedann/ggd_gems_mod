package su.ggd.ggd_gems_mod.quests.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.quests.config.QuestsDatabase;
import su.ggd.ggd_gems_mod.quests.service.QuestService;
import su.ggd.ggd_gems_mod.quests.state.QuestsState;

import java.util.ArrayList;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class QuestsCommands {
    private QuestsCommands() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("quests")
                .then(literal("reload")
                        .executes(ctx -> {
                            QuestsConfigManager.loadOrCreateDefault();
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity p = src.getPlayer();
                            if (p != null) {
                                // defs + state sync on reload for caller
                                // defs sync already on join; we just force refresh now
                                src.sendFeedback(() -> Text.literal("[Quests] Reloaded quests folder."), false);
                            } else {
                                src.sendFeedback(() -> Text.literal("[Quests] Reloaded quests folder."), false);
                            }
                            return 1;
                        }))

                .then(literal("start")
                        .then(argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean ok = QuestService.startQuest(p, id);
                                    ctx.getSource().sendFeedback(() -> Text.literal(ok
                                            ? "[Quests] Started: " + id
                                            : "[Quests] Cannot start: " + id), false);
                                    return ok ? 1 : 0;
                                })))

                .then(literal("abandon")
                        .then(argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean ok = QuestService.abandonQuest(p, id);
                                    ctx.getSource().sendFeedback(() -> Text.literal(ok
                                            ? "[Quests] Abandoned: " + id
                                            : "[Quests] Cannot abandon: " + id), false);
                                    return ok ? 1 : 0;
                                })))

                .then(literal("track")
                        .then(argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    // suggest active quest ids + "none"
                                    try {
                                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                                        if (p != null) {
                                            QuestsState st = QuestsState.get((net.minecraft.server.world.ServerWorld) p.getEntityWorld());
                                            QuestsState.PlayerQuestData pd = st.getOrCreate(p.getUuid());
                                            for (String qid : pd.activeQuests.keySet()) builder.suggest(qid);
                                        }
                                    } catch (Exception ignored) {}
                                    builder.suggest("none");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean ok = QuestService.trackQuest(p, id);
                                    ctx.getSource().sendFeedback(() -> Text.literal(ok
                                            ? "[Quests] Tracked: " + id
                                            : "[Quests] Cannot track: " + id), false);
                                    return ok ? 1 : 0;
                                })))

                .then(literal("reset")
                        .then(argument("id", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    // suggest active + completed + "all"
                                    try {
                                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                                        if (p != null) {
                                            QuestsState st = QuestsState.get((net.minecraft.server.world.ServerWorld) p.getEntityWorld());
                                            QuestsState.PlayerQuestData pd = st.getOrCreate(p.getUuid());
                                            for (String qid : pd.activeQuests.keySet()) builder.suggest(qid);
                                            for (String qid : pd.completedQuests) builder.suggest(qid);
                                        }
                                    } catch (Exception ignored) {}
                                    builder.suggest("all");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");

                                    var w = (net.minecraft.server.world.ServerWorld) p.getEntityWorld();
                                    QuestsState st = QuestsState.get(w);
                                    QuestsState.PlayerQuestData pd = st.getOrCreate(p.getUuid());

                                    if ("all".equalsIgnoreCase(id)) {
                                        pd.activeQuests.clear();
                                        pd.completedQuests.clear();
                                        pd.unlockedQuests.clear();
                                        pd.flags.clear();
                                        pd.trackedQuestId = null;
                                        st.markDirty();
                                        QuestService.sync(p);
                                        ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Reset ALL data."), false);
                                        return 1;
                                    }

                                    boolean removed = (pd.activeQuests.remove(id) != null);
                                    boolean removedCompleted = pd.completedQuests.remove(id);
                                    boolean removedUnlocked = pd.unlockedQuests.remove(id);

                                    if (id.equals(pd.trackedQuestId)) pd.trackedQuestId = null;

                                    if (removed || removedCompleted || removedUnlocked) {
                                        st.markDirty();
                                        QuestService.sync(p);
                                        ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Reset: " + id), false);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Nothing to reset: " + id), false);
                                        return 0;
                                    }
                                })))

                .then(literal("debug")
                        .then(argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    String id = StringArgumentType.getString(ctx, "id");

                                    QuestsDatabase.QuestFullDef def = QuestsConfigManager.get().get(id);
                                    if (def == null) {
                                        ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Unknown quest: " + id), false);
                                        return 0;
                                    }

                                    String json = GSON.toJson(def);
                                    ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Def: " + id), false);
                                    ctx.getSource().sendFeedback(() -> Text.literal(json), false);

                                    var w = (net.minecraft.server.world.ServerWorld) p.getEntityWorld();
                                    QuestsState st = QuestsState.get(w);
                                    QuestsState.PlayerQuestData pd = st.getOrCreate(p.getUuid());

                                    ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Active: " + pd.activeQuests.keySet()), false);
                                    ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Completed: " + pd.completedQuests), false);
                                    ctx.getSource().sendFeedback(() -> Text.literal("[Quests] Tracked: " + pd.trackedQuestId), false);

                                    return 1;
                                })))
        );
    }
}
