package su.ggd.ggd_gems_mod.npc;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;

public final class NpcDamageBlocker {
    private NpcDamageBlocker() {}

    private static boolean DONE = false;

    public static void init() {
        if (DONE) return;
        DONE = true;

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (entity == null) return ActionResult.PASS;

            for (String tag : entity.getCommandTags()) {
                if (tag != null && tag.startsWith(GemTraderNpc.TAG_PREFIX)) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });
    }
}
