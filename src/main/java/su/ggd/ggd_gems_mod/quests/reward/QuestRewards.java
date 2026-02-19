package su.ggd.ggd_gems_mod.quests.reward;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.quests.config.QuestDef;
import su.ggd.ggd_gems_mod.currency.CurrencyService;


public final class QuestRewards {
    private QuestRewards() {}

    public static void give(ServerPlayerEntity player, QuestDef.RewardBundle bundle) {
        if (player == null || bundle == null || bundle.rewards == null) return;

        for (QuestDef.Reward r : bundle.rewards) {
            if (r == null || r.type == null) continue;

            String t = r.type.trim().toUpperCase();
            switch (t) {
                case "XP" -> {
                    int xp = r.count == null ? 0 : r.count;
                    if (xp > 0) player.addExperience(xp);
                }
                case "ITEMS" -> {
                    String itemId = r.id;
                    int count = r.count == null ? 0 : r.count;
                    if (itemId == null || itemId.isBlank() || count <= 0) break;

                    Item item = resolveItem(itemId);
                    if (item == null) break;

                    ItemStack stack = new ItemStack(item, count);
                    boolean ok = player.getInventory().insertStack(stack);
                    if (!ok && !stack.isEmpty()) {
                        player.dropItem(stack, false);
                    }
                }
                case "MESSAGE" -> {
                    String msg = r.message;
                    if (msg == null || msg.isBlank()) break;
                    player.sendMessage(Text.literal(msg));
                }
                case "CURRENCY" -> {
                    int amount = r.count == null ? 0 : r.count;
                    if (amount > 0) CurrencyService.add(player, amount);
                }

                default -> {
                    // V1: остальные типы позже (CURRENCY, SKILL_POINTS, SET_PROFESSION, COMMAND...)
                }
            }
        }
    }

    private static Item resolveItem(String itemId) {
        Identifier id;
        try {
            id = Identifier.of(itemId);
        } catch (Exception e) {
            return null;
        }
        if (!Registries.ITEM.containsId(id)) return null;
        return Registries.ITEM.get(id);
    }
}
