package su.ggd.ggd_gems_mod.init.client;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.PlayerStatsScreen;
import su.ggd.ggd_gems_mod.client.menu.RpgMenuPages;
import su.ggd.ggd_gems_mod.client.menu.bank.BankScreen;
import su.ggd.ggd_gems_mod.client.menu.trader.TraderScreen;
import su.ggd.ggd_gems_mod.passive.PassiveSkillsScreen;
import su.ggd.ggd_gems_mod.quests.client.ui.QuestJournalScreen;
import su.ggd.ggd_gems_mod.util.InitOnce;

public final class ClientMenuInit {
    private ClientMenuInit() {}

    public static void init() {
        if (!InitOnce.markDone("ClientMenuInit")) return;

        RpgMenuPages.register("stats", Text.literal("Статы"), new ItemStack(Items.IRON_SWORD), PlayerStatsScreen::new);
        RpgMenuPages.register("passives", Text.literal("Пассивки"), new ItemStack(Items.ENCHANTED_BOOK), PassiveSkillsScreen::new);
        RpgMenuPages.register("quests", Text.literal("Квесты"), new ItemStack(Items.BOOK), QuestJournalScreen::new);

        RpgMenuPages.register("bank", Text.literal("Банк"), new ItemStack(Items.GOLD_INGOT), BankScreen::new);

        // торговля NPC
        RpgMenuPages.register("trader", Text.literal("Торговля"), new ItemStack(Items.EMERALD), TraderScreen::new);
    }
}
