package su.ggd.ggd_gems_mod.init;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import su.ggd.ggd_gems_mod.passive.PassiveSkillHooks;
import su.ggd.ggd_gems_mod.quests.QuestHooks;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ServerTickInit {
    private ServerTickInit() {}

    public static void init() {
        if (!InitOnce.markDone("ServerTickInit")) return;

        ServerTickEvents.END_SERVER_TICK.register(ServerTickInit::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        PassiveSkillHooks.tick(server);
        QuestHooks.tick(server);
    }
}
