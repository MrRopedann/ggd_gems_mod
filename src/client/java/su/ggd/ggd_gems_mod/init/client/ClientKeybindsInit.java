package su.ggd.ggd_gems_mod.init.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import su.ggd.ggd_gems_mod.client.menu.RpgMenuScreen;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientKeybindsInit {
    private ClientKeybindsInit() {}

    private static KeyBinding OPEN_MENU;

    public static void init() {
        if (!InitOnce.markDone("ClientKeybindsInit")) return;

        OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.open_rpg_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.MISC
        ));
    }

    public static void tick(MinecraftClient client) {
        while (OPEN_MENU != null && OPEN_MENU.wasPressed()) {
            if (client.player != null) {
                // null => пусть меню само выберет первую/последнюю вкладку
                RpgMenuScreen.open(client, null);
            }
        }
    }
}
