package su.ggd.ggd_gems_mod.npc;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.util.ActionResult;

public final class NpcDamageBlocker {
    private NpcDamageBlocker() {}

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (entity == null) return ActionResult.PASS;

            // Запрещаем любые атаки по NPC нашего мода
            for (String tag : entity.getCommandTags()) {
                if (tag != null && tag.startsWith(GemTraderNpc.TAG_PREFIX)) {
                    return ActionResult.FAIL; // полностью блокируем удар
                }
            }

            return ActionResult.PASS;
        });
    }
}
