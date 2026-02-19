package su.ggd.ggd_gems_mod.npc;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import su.ggd.ggd_gems_mod.npc.server.NpcTraderServerHandlers;

public final class NpcTraderInteractHook {
    private NpcTraderInteractHook() {}

    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

            String traderId = getTraderId(entity);
            if (traderId == null) return ActionResult.PASS;

            // Открываем кастомный UI и блокируем ванильную торговлю
            NpcTraderServerHandlers.sendOpen(sp, traderId);
            return ActionResult.SUCCESS;
        });
    }

    private static String getTraderId(Entity e) {
        if (e == null) return null;
        for (String tag : e.getCommandTags()) {
            if (tag == null) continue;
            if (tag.startsWith(GemTraderNpc.TAG_PREFIX)) {
                String id = tag.substring(GemTraderNpc.TAG_PREFIX.length()).trim();
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }
}
