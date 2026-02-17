package su.ggd.ggd_gems_mod.quests.client.ui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class QuestJournalClient {
    private QuestJournalClient() {}

    private static KeyBinding OPEN_KEY;

    public static void init() {
        OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.open_quests",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                net.minecraft.client.option.KeyBinding.Category.MISC
        ));


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new QuestJournalScreen());
                }
            }
        });
    }
}
