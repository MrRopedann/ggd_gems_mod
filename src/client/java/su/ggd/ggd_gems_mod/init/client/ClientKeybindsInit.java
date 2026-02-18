package su.ggd.ggd_gems_mod.init.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import su.ggd.ggd_gems_mod.PlayerStatsScreen;
import su.ggd.ggd_gems_mod.passive.PassiveSkillsScreen;
import su.ggd.ggd_gems_mod.quests.client.ui.QuestJournalScreen;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientKeybindsInit {
    private ClientKeybindsInit() {}

    private static KeyBinding OPEN_STATS;
    private static KeyBinding OPEN_PASSIVES;
    private static KeyBinding OPEN_QUESTS;

    public static void init() {
        if (!InitOnce.markDone("ClientKeybindsInit")) return;

        OPEN_STATS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.open_stats",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.MISC
        ));

        OPEN_PASSIVES = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.open_passives",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyBinding.Category.MISC
        ));

        OPEN_QUESTS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.open_quests",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_STATS.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.setScreen(new PlayerStatsScreen());
            }
            while (OPEN_PASSIVES.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.setScreen(new PassiveSkillsScreen());
            }
            while (OPEN_QUESTS.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) mc.setScreen(new QuestJournalScreen());
            }
        });
    }
}
