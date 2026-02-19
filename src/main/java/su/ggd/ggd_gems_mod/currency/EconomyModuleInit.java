package su.ggd.ggd_gems_mod.currency;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class EconomyModuleInit {
    private EconomyModuleInit() {}

    public static void init() {
        if (!InitOnce.markDone("EconomyModuleInit")) return;

        PiastreDrops.register();

        // ПКМ по воздуху с пиастрами: стек исчезает, баланс увеличивается.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (!(world instanceof ServerWorld)) return ActionResult.PASS;

            var stack = player.getStackInHand(hand);
            if (stack == null || stack.isEmpty()) return ActionResult.PASS;

            var currencyItem = CurrencyItems.getCurrencyItemOrNull();
            if (currencyItem == null) return ActionResult.PASS;
            if (!stack.isOf(currencyItem)) return ActionResult.PASS;

            int count = stack.getCount();
            if (count <= 0) return ActionResult.PASS;

            stack.decrement(count);
            CurrencyService.add(sp, count);
            sp.sendMessage(Text.literal("+" + count + " монет (на счёт)"), true);
            return ActionResult.SUCCESS;
        });
    }
}
