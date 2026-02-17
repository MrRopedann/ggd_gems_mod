package su.ggd.ggd_gems_mod.quests.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import su.ggd.ggd_gems_mod.quests.client.QuestClientState;
import su.ggd.ggd_gems_mod.quests.client.net.QuestClientNet;
import su.ggd.ggd_gems_mod.quests.config.ObjectiveDef;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.quests.config.QuestsDatabase;
import su.ggd.ggd_gems_mod.quests.config.StageDef;
import su.ggd.ggd_gems_mod.quests.state.QuestsState;

import java.util.*;

public final class QuestJournalScreen extends Screen {

    private enum Tab { ACTIVE, AVAILABLE, COMPLETED }

    private Tab tab = Tab.ACTIVE;

    private int scroll = 0;
    private static final int ROW_H = 34;

    private ButtonWidget tabActive;
    private ButtonWidget tabAvailable;
    private ButtonWidget tabCompleted;

    // ВАЖНО: храним последний отрисованный список и кликаем по нему
    private List<Row> lastRenderedRows = List.of();

    public QuestJournalScreen() {
        super(Text.literal("Quests"));
    }

    @Override
    protected void init() {
        int x = 10;
        int y = 12;

        tabActive = ButtonWidget.builder(Text.literal("Active"), b -> { tab = Tab.ACTIVE; scroll = 0; })
                .dimensions(x, y, 70, 20).build();
        tabAvailable = ButtonWidget.builder(Text.literal("Available"), b -> { tab = Tab.AVAILABLE; scroll = 0; })
                .dimensions(x + 74, y, 90, 20).build();
        tabCompleted = ButtonWidget.builder(Text.literal("Completed"), b -> { tab = Tab.COMPLETED; scroll = 0; })
                .dimensions(x + 168, y, 90, 20).build();

        addDrawableChild(tabActive);
        addDrawableChild(tabAvailable);
        addDrawableChild(tabCompleted);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // используем lastRenderedRows чтобы не пересобирать rows на каждом скролле
        int max = Math.max(0, lastRenderedRows.size() - getVisibleRowCount());
        scroll = MathHelper.clamp(scroll - (int) Math.signum(verticalAmount), 0, max);
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // фон без blur (иначе "Can only blur once per frame")
        ctx.fill(0, 0, width, height, 0xA0000000);

        // header bar
        ctx.fill(8, 8, width - 8, 40, 0x90000000);

        super.render(ctx, mouseX, mouseY, delta);

        // highlight active tab
        drawTabHighlight(ctx);

        List<Row> rows = getRows();
        this.lastRenderedRows = rows; // <-- ключевая строка

        int left = 10;
        int top = 46;
        int right = width - 10;
        int bottom = height - 14;

        ctx.fill(left, top, right, bottom, 0x70000000);

        int visible = getVisibleRowCount();
        int maxScroll = Math.max(0, rows.size() - visible);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        int start = scroll;
        int end = Math.min(rows.size(), start + visible);

        int y = top + 6;
        for (int i = start; i < end; i++) {
            Row r = rows.get(i);
            renderRow(ctx, r, left + 6, y, right - 6, mouseX, mouseY);
            y += ROW_H;
        }

        // footer
        String s = "Rows: " + rows.size();
        ctx.drawTextWithShadow(textRenderer, Text.literal(s), 12, height - 11, 0xFFFFFFFF);
    }

    private void drawTabHighlight(DrawContext ctx) {
        ButtonWidget b = switch (tab) {
            case ACTIVE -> tabActive;
            case AVAILABLE -> tabAvailable;
            case COMPLETED -> tabCompleted;
        };
        ctx.fill(b.getX() - 1, b.getY() - 1, b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 1, 0x60FFFFFF);
    }

    private int getVisibleRowCount() {
        int top = 46;
        int bottom = height - 14;
        int usable = (bottom - top - 12);
        return Math.max(1, usable / ROW_H);
    }

    private void renderRow(DrawContext ctx, Row row, int left, int y, int right, int mouseX, int mouseY) {
        int rowTop = y;
        int rowBottom = y + ROW_H - 2;

        boolean tracked = row.questId != null && row.questId.equals(getPlayerData().trackedQuestId);

        ctx.fill(left, rowTop, right, rowBottom, tracked ? 0x5533AAFF : 0x50000000);

        if (!row.icon.isEmpty()) {
            ctx.drawItem(row.icon, left + 4, rowTop + 8);
        }

        int tx = left + 26;
        ctx.drawTextWithShadow(textRenderer, Text.literal(row.title), tx, rowTop + 6, 0xFFFFFFFF);

        if (row.subtitle != null && !row.subtitle.isBlank()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(row.subtitle), tx, rowTop + 18, 0xFFD0D0D0);
        }

        // right-side buttons
        int bx = right - 4;
        int by = rowTop + 6;

        List<ActionButton> buttons = row.buttons;
        for (int i = buttons.size() - 1; i >= 0; i--) {
            ActionButton ab = buttons.get(i);

            int h = 16;
            bx -= ab.w;
            ab.x = bx;
            ab.y = by;
            ab.h = h;

            boolean hover = isMouseOver(mouseX, mouseY, ab.x, ab.y, ab.w, ab.h);

            // стиль "активной" кнопки
            int bg = hover ? 0xC03333AA : 0xA0222266;
            ctx.fill(ab.x, ab.y, ab.x + ab.w, ab.y + ab.h, bg);
            ctx.drawTextWithShadow(textRenderer, Text.literal(ab.label), ab.x + 4, ab.y + 4, 0xFFFFFFFF);

            bx -= 6;
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        double mouseX = click.x();
        double mouseY = click.y();

        // ВАЖНО: кликаем по lastRenderedRows, а не getRows()
        List<Row> rows = this.lastRenderedRows;
        if (rows == null || rows.isEmpty()) return super.mouseClicked(click, doubled);

        int visible = getVisibleRowCount();
        int start = scroll;
        int end = Math.min(rows.size(), start + visible);

        int y = 46 + 6;
        for (int i = start; i < end; i++) {
            Row r = rows.get(i);
            for (ActionButton ab : r.buttons) {
                if (isMouseOver(mouseX, mouseY, ab.x, ab.y, ab.w, ab.h)) {
                    ab.run();
                    return true;
                }
            }
            y += ROW_H;
        }

        return super.mouseClicked(click, doubled);
    }

    private static boolean isMouseOver(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private QuestsState.PlayerQuestData getPlayerData() {
        QuestsState.PlayerQuestData pd = QuestClientState.getPlayerData();
        return pd != null ? pd : new QuestsState.PlayerQuestData();
    }

    private List<Row> getRows() {
        QuestsDatabase db = QuestsConfigManager.get();
        if (db == null) return List.of();
        if (db.byId == null || db.byId.isEmpty()) db.rebuildIndex();

        QuestsState.PlayerQuestData pd = getPlayerData();

        Set<String> activeIds = pd.activeQuests.keySet();
        Set<String> completedIds = pd.completedQuests;

        ArrayList<Row> rows = new ArrayList<>();

        if (tab == Tab.ACTIVE) {
            for (String qid : activeIds) {
                QuestsDatabase.QuestFullDef qdef = db.get(qid);
                if (qdef == null || qdef.quest == null) continue;

                Row r = new Row(qid);
                r.title = safe(qdef.quest.name, qid);
                r.subtitle = buildActiveSubtitle(qdef, pd.activeQuests.get(qid));
                r.icon = resolveIcon(qdef.quest.iconItem);

                r.buttons.add(ActionButton.track(qid));
                r.buttons.add(ActionButton.abandon(qid));
                rows.add(r);
            }
        } else if (tab == Tab.COMPLETED) {
            for (String qid : completedIds) {
                QuestsDatabase.QuestFullDef qdef = db.get(qid);
                if (qdef == null || qdef.quest == null) continue;

                Row r = new Row(qid);
                r.title = safe(qdef.quest.name, qid);
                r.subtitle = "Completed";
                r.icon = resolveIcon(qdef.quest.iconItem);

                r.buttons.add(ActionButton.track(qid));
                rows.add(r);
            }
        } else {
            for (String qid : db.byId.keySet()) {
                if (activeIds.contains(qid)) continue;
                if (completedIds.contains(qid)) continue;

                QuestsDatabase.QuestFullDef qdef = db.get(qid);
                if (qdef == null || qdef.quest == null) continue;

                Row r = new Row(qid);
                r.title = safe(qdef.quest.name, qid);
                r.subtitle = safe(qdef.quest.description, "");
                r.icon = resolveIcon(qdef.quest.iconItem);

                r.buttons.add(ActionButton.start(qid));
                rows.add(r);
            }
        }

        rows.sort(Comparator.comparing(a -> a.title.toLowerCase(Locale.ROOT)));
        return rows;
    }

    private static String buildActiveSubtitle(QuestsDatabase.QuestFullDef qdef, QuestsState.ActiveQuestProgress pr) {
        if (qdef == null || pr == null) return "";
        StageDef stage = findStage(qdef, pr.currentStageId);
        if (stage == null) return "Stage: " + pr.currentStageId;

        int reqSum = 0;
        int curSum = 0;
        if (stage.objectives != null) {
            for (ObjectiveDef o : stage.objectives) {
                if (o == null || o.hidden) continue;
                int req = Math.max(1, o.required);
                int cur = pr.objectives.getOrDefault(o.id, 0);
                reqSum += req;
                curSum += Math.min(cur, req);
            }
        }

        String stageName = safe(stage.name, stage.id);
        return stageName + "  (" + curSum + "/" + Math.max(1, reqSum) + ")";
    }

    private static StageDef findStage(QuestsDatabase.QuestFullDef def, String stageId) {
        if (def == null || def.stages == null || stageId == null) return null;
        for (StageDef s : def.stages) {
            if (s != null && stageId.equals(s.id)) return s;
        }
        return null;
    }

    private static ItemStack resolveIcon(String itemId) {
        if (itemId == null || itemId.isBlank()) return ItemStack.EMPTY;
        Identifier id;
        try { id = Identifier.of(itemId); } catch (Exception e) { return ItemStack.EMPTY; }
        if (!Registries.ITEM.containsId(id)) return ItemStack.EMPTY;
        Item it = Registries.ITEM.get(id);
        if (it == null) return ItemStack.EMPTY;
        return new ItemStack(it);
    }

    private static String safe(String s, String fallback) {
        if (s == null || s.isBlank()) return fallback;
        return s;
    }

    // --------- small structs ---------

    private static final class Row {
        final String questId;
        String title = "";
        String subtitle = "";
        ItemStack icon = ItemStack.EMPTY;
        final List<ActionButton> buttons = new ArrayList<>();
        Row(String questId) { this.questId = questId; }
    }

    private static final class ActionButton {
        final String label;
        final int w;
        int x, y, h;
        final Runnable action;

        ActionButton(String label, int w, Runnable action) {
            this.label = label;
            this.w = w;
            this.action = action;
        }

        void run() { action.run(); }

        static ActionButton start(String questId) {
            return new ActionButton("Start", 42, () -> QuestClientNet.startQuest(questId));
        }

        static ActionButton abandon(String questId) {
            return new ActionButton("Abandon", 58, () -> QuestClientNet.abandonQuest(questId));
        }

        static ActionButton track(String questId) {
            return new ActionButton("Track", 44, () -> QuestClientNet.trackQuest(questId));
        }
    }
}
