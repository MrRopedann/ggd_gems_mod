package su.ggd.ggd_gems_mod.inventory.sort.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import su.ggd.ggd_gems_mod.inventory.sort.InventorySortType;
import su.ggd.ggd_gems_mod.inventory.sort.net.InventorySortNet;

public final class InventorySortClient {
    private InventorySortClient() {}

    private static KeyBinding SORT_KEY;
    private static InventorySortType current = InventorySortType.NAME;

    public static void init() {
        SORT_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ggd_gems_mod.sort_inventory",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyBinding.Category.INVENTORY   // ✅ ВАЖНО
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SORT_KEY.wasPressed()) {
                sendSort(client);
            }
        });
    }


    public static void cycleMode(MinecraftClient client, boolean notify) {
        current = next(current);
        if (notify && client != null && client.player != null) {
            client.player.sendMessage(
                    Text.translatable("text.ggd_gems_mod.sort_mode", current.name()),
                    true
            );
        }
    }

    private static void sendSort(MinecraftClient client) {
        if (client == null || client.player == null) return;

        // Shift + R — переключить режим сортировки
        if (client.player.isSneaking()) {
            cycleMode(client, true);
            return;
        }


        ClientPlayNetworking.send(
                new InventorySortNet.SortRequestPayload(current.toWire())
        );
    }

    private static InventorySortType next(InventorySortType t) {
        InventorySortType[] vals = InventorySortType.values();
        return vals[(t.ordinal() + 1) % vals.length];
    }

    public static InventorySortType getCurrentSortType() {
        return current;
    }

}
