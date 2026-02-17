package su.ggd.ggd_gems_mod.quests;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.jetbrains.annotations.Nullable;

public final class TalkHook {
    private TalkHook() {}

    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

            String npcId = getNpcId(entity);
            if (npcId == null) return ActionResult.PASS;

            QuestDispatcher.onTalk(sw, sp, npcId);
            return ActionResult.PASS;
        });
    }

    private static @Nullable String getNpcId(Entity e) {
        Text name = e.getCustomName();
        if (name == null) return null;
        String s = name.getString();
        if (s == null) return null;
        s = s.trim();
        if (!s.startsWith("npc:")) return null;
        String id = s.substring("npc:".length()).trim();
        return id.isEmpty() ? null : id;
    }
}
