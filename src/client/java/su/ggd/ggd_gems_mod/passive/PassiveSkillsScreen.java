package su.ggd.ggd_gems_mod.passive;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;

import java.util.*;

public class PassiveSkillsScreen extends Screen {

    private static final int BASE_PANEL_W = 360;
    private static final int BASE_PANEL_H = 240;

    private static final int PAD_X = 12;

    private static final int ICON_BOX = 20;
    private static final int BTN_W = 150;
    private static final int BTN_H = 20;

    // Tabs
    private static final int TAB_H = 18;
    private static final int TAB_GAP = 4;
    private static final int HEADER_H = 72; // title + player lvl + tabs + padding

    // Rows
    private static final int ROW_H = 64;
    private static final int DESC_LINE_H = 10;

    // scrollbar
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_PAD = 6;
    private static final int SCROLLBAR_MIN_THUMB_H = 14;

    private final Map<String, ButtonWidget> upgradeButtons = new HashMap<>();
    private final Map<String, ButtonWidget> tabButtons = new HashMap<>();

    // локальное сообщение (рисуется в окне)
    private Text uiMessage = null;
    private int uiMessageTicks = 0;

    // selected tab
    private String selectedTabId = "all";

    // visible list for current tab
    private List<PassiveSkillsConfig.PassiveSkillDef> visibleSkills = Collections.emptyList();

    // scroll in rows
    private int scroll = 0;

    // drag scrollbar
    private boolean draggingScrollbar = false;
    private int dragStartMouseY = 0;
    private int dragStartScroll = 0;

    public PassiveSkillsScreen() {
        super(Text.literal("Пассивные навыки"));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // no-op
    }

    @Override
    protected void init() {
        super.init();

        upgradeButtons.clear();
        tabButtons.clear();

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null) return;

        // rebuild visible list
        visibleSkills = computeVisibleSkills(cfg);

        int panelW = Math.min(BASE_PANEL_W, this.width - 40);
        int panelH = Math.min(BASE_PANEL_H, this.height - 40);
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // ---- Tabs (buttons) ----
        // Always have "all"
        List<PassiveSkillsConfig.TabDef> tabs = new ArrayList<>();
        PassiveSkillsConfig.TabDef all = new PassiveSkillsConfig.TabDef();
        all.id = "all";
        all.name = "Все";
        all.iconItem = null;
        all.skillIds = Collections.emptyList();
        tabs.add(all);

        if (cfg.tabs != null) tabs.addAll(cfg.tabs);

        int tabsX = left + PAD_X;
        int tabsY = top + 46; // under title + player lv
        int tabsAreaW = panelW - PAD_X * 2;

        // simple equal width buttons
        int count = Math.max(1, tabs.size());
        int tabW = Math.max(44, (tabsAreaW - (count - 1) * TAB_GAP) / count);

        for (int i = 0; i < tabs.size(); i++) {
            PassiveSkillsConfig.TabDef t = tabs.get(i);
            if (t == null || t.id == null) continue;

            int x = tabsX + i * (tabW + TAB_GAP);
            ButtonWidget tabBtn = ButtonWidget.builder(
                    Text.literal((t.name == null || t.name.isBlank()) ? t.id : t.name),
                    b -> {
                        selectedTabId = t.id;
                        scroll = 0;
                        this.clearChildren();
                        this.init();
                    }
            ).dimensions(x, tabsY, tabW, TAB_H).build();

            addDrawableChild(tabBtn);
            tabButtons.put(t.id, tabBtn);
        }

        // ---- Content + upgrade buttons (visible only) ----
        ScrollContext sc = computeScrollContext(visibleSkills.size());
        scroll = MathHelper.clamp(scroll, 0, sc.maxScroll);

        int start = scroll;
        int end = Math.min(visibleSkills.size(), start + sc.visibleRows);

        int y = sc.contentY;

        for (int idx = start; idx < end; idx++) {
            PassiveSkillsConfig.PassiveSkillDef def = visibleSkills.get(idx);
            if (def == null) {
                y += ROW_H;
                continue;
            }

            int rowRight = left + panelW - PAD_X;
            int btnX = rowRight - BTN_W;
            int btnY = y + (ROW_H - BTN_H) / 2;

            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal("Улучшить"),
                    b -> {
                        ClientPlayNetworking.send(new PassiveSkillsNet.UpgradePassiveSkillPayload(def.id));
                        showUiMessage(Text.literal("Запрос улучшения отправлен..."), 40);
                    }
            ).dimensions(btnX, btnY, BTN_W, BTN_H).build();

            addDrawableChild(btn);
            upgradeButtons.put(def.id, btn);

            y += ROW_H;
        }

        refreshButtons();
        refreshTabsVisual();
    }

    private void refreshTabsVisual() {
        for (var e : tabButtons.entrySet()) {
            String id = e.getKey();
            ButtonWidget b = e.getValue();
            if (b == null) continue;
            b.active = true;

            // визуально выделим выбранную вкладку: просто меняем текст (без кастом-рендера)
            String base = b.getMessage().getString();
            if (Objects.equals(id, selectedTabId)) {
                if (!base.startsWith("> ")) b.setMessage(Text.literal("> " + base));
            } else {
                if (base.startsWith("> ")) b.setMessage(Text.literal(base.substring(2)));
            }
        }
    }

    private List<PassiveSkillsConfig.PassiveSkillDef> computeVisibleSkills(PassiveSkillsConfig cfg) {
        if (cfg.skills == null) return Collections.emptyList();

        if ("all".equals(selectedTabId) || cfg.tabs == null || cfg.tabs.isEmpty()) {
            return new ArrayList<>(cfg.skills);
        }

        PassiveSkillsConfig.TabDef tab = null;
        for (var t : cfg.tabs) {
            if (t != null && Objects.equals(t.id, selectedTabId)) {
                tab = t;
                break;
            }
        }
        if (tab == null || tab.skillIds == null || tab.skillIds.isEmpty()) {
            return new ArrayList<>(cfg.skills);
        }

        // Build byId if not present
        Map<String, PassiveSkillsConfig.PassiveSkillDef> byId = cfg.byId;
        if (byId == null || byId.isEmpty()) {
            byId = new HashMap<>();
            for (var s : cfg.skills) if (s != null && s.id != null) byId.put(s.id, s);
        }

        List<PassiveSkillsConfig.PassiveSkillDef> out = new ArrayList<>();
        for (String id : tab.skillIds) {
            if (id == null) continue;
            PassiveSkillsConfig.PassiveSkillDef def = byId.get(id);
            if (def != null) out.add(def);
        }
        return out;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (visibleSkills == null) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        ScrollContext sc = computeScrollContext(visibleSkills.size());
        if (sc.maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        int delta = (int) Math.signum(verticalAmount);
        if (delta != 0) {
            scroll = MathHelper.clamp(scroll - delta, 0, sc.maxScroll);
            this.clearChildren();
            this.init();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ===== NEW INPUT SIGNATURES (1.21.11) =====

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        // scrollbar interactions first
        if (click.button() == 0 && isMouseOverScrollbar((int) click.x(), (int) click.y())) {
            ScrollContext sc = computeScrollContext(visibleSkills == null ? 0 : visibleSkills.size());
            if (sc.maxScroll > 0) {
                ScrollbarGeom g = computeScrollbarGeom(sc, visibleSkills.size());

                int my = (int) click.y();
                if (my >= g.thumbY && my <= g.thumbY + g.thumbH) {
                    draggingScrollbar = true;
                    dragStartMouseY = my;
                    dragStartScroll = scroll;
                    return true;
                } else {
                    // click on track => jump
                    float t = (float) (my - g.trackY) / (float) Math.max(1, g.trackH);
                    int target = Math.round(t * sc.maxScroll);
                    scroll = MathHelper.clamp(target, 0, sc.maxScroll);
                    this.clearChildren();
                    this.init();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (click.button() == 0 && draggingScrollbar) {
            ScrollContext sc = computeScrollContext(visibleSkills == null ? 0 : visibleSkills.size());
            if (sc.maxScroll <= 0) return true;

            ScrollbarGeom g = computeScrollbarGeom(sc, visibleSkills.size());

            int my = (int) click.y();
            int dy = my - dragStartMouseY;

            int travel = Math.max(1, g.trackH - g.thumbH);
            float pixelsPerStep = (float) travel / (float) sc.maxScroll;

            int stepDelta = Math.round(dy / Math.max(1e-3f, pixelsPerStep));
            int next = MathHelper.clamp(dragStartScroll + stepDelta, 0, sc.maxScroll);

            if (next != scroll) {
                scroll = next;
                this.clearChildren();
                this.init();
            }
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public void tick() {
        super.tick();

        if (uiMessageTicks > 0) {
            uiMessageTicks--;
            if (uiMessageTicks == 0) uiMessage = null;
        }

        refreshButtons();
        refreshTabsVisual();
    }

    private void refreshButtons() {
        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || mc.player == null) return;

        int playerLevel = mc.player.experienceLevel;

        // обновляем только существующие (видимые) кнопки
        for (PassiveSkillsConfig.PassiveSkillDef def : visibleSkills) {
            if (def == null) continue;

            ButtonWidget btn = upgradeButtons.get(def.id);
            if (btn == null) continue;

            int cur = ClientPassiveSkills.getLevel(def.id);
            int next = cur + 1;
            int cost = costForNext(def, next);

            boolean canUpgrade = next <= def.maxLevel && playerLevel >= cost;

            btn.active = canUpgrade;
            btn.setMessage(Text.literal("Улучшить (" + cost + " ур.)"));
        }
    }

    private void showUiMessage(Text msg, int ticks) {
        this.uiMessage = msg;
        this.uiMessageTicks = ticks;
    }

    private List<Text> wrapStringToTextLines(String s, int maxWidth) {
        List<Text> out = new ArrayList<>();
        if (s == null) return out;

        String[] words = s.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            if (w.isBlank()) continue;

            String candidate = line.isEmpty() ? w : (line + " " + w);
            if (textRenderer.getWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) out.add(Text.literal(line.toString()));
                line.setLength(0);

                // если слово само длиннее maxWidth — режем по ширине
                if (textRenderer.getWidth(w) > maxWidth) {
                    String cut = textRenderer.trimToWidth(w, maxWidth);
                    out.add(Text.literal(cut));
                } else {
                    line.append(w);
                }
            }
        }

        if (!line.isEmpty()) out.add(Text.literal(line.toString()));
        return out;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);

        int panelW = Math.min(BASE_PANEL_W, this.width - 40);
        int panelH = Math.min(BASE_PANEL_H, this.height - 40);
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // panel + border
        context.fill(left, top, left + panelW, top + panelH, 0xCC000000);
        context.fill(left, top, left + panelW, top + 1, 0xFF777777);
        context.fill(left, top + panelH - 1, left + panelW, top + panelH, 0xFF777777);
        context.fill(left, top, left + 1, top + panelH, 0xFF777777);
        context.fill(left + panelW - 1, top, left + panelW, top + panelH, 0xFF777777);

        // widgets (tabs + buttons)
        super.render(context, mouseX, mouseY, delta);

        // header
        context.drawTextWithShadow(textRenderer, this.title, left + 12, top + 12, 0xFFFFFFFF);

        MinecraftClient mc = MinecraftClient.getInstance();
        int playerLevel = mc.player == null ? 0 : mc.player.experienceLevel;
        context.drawTextWithShadow(textRenderer, Text.literal("Уровни игрока: " + playerLevel), left + 12, top + 26, 0xFFAAAAAA);

        // local message
        if (uiMessage != null) {
            int msgW = textRenderer.getWidth(uiMessage);
            int msgX = left + (panelW - msgW) / 2;
            int msgY = top + 36;
            context.drawTextWithShadow(textRenderer, uiMessage, msgX, msgY, 0xFFFFFF55);
        }

        if (visibleSkills == null) return;

        ScrollContext sc = computeScrollContext(visibleSkills.size());
        scroll = MathHelper.clamp(scroll, 0, sc.maxScroll);

        int start = scroll;
        int end = Math.min(visibleSkills.size(), start + sc.visibleRows);

        int y = sc.contentY;

        // for tooltip
        PassiveSkillsConfig.PassiveSkillDef hoveredDef = null;
        int hoveredRowX0 = 0, hoveredRowY0 = 0, hoveredRowX1 = 0, hoveredRowY1 = 0;

        // draw rows (visible range)
        for (int idx = start; idx < end; idx++) {
            PassiveSkillsConfig.PassiveSkillDef def = visibleSkills.get(idx);
            if (def == null) {
                y += ROW_H;
                continue;
            }

            int rowLeft = left + PAD_X;
            int rowRight = left + panelW - PAD_X;

            // hover rect (excluding header)
            int rowX0 = rowLeft;
            int rowY0 = y;
            int rowX1 = rowRight - (SCROLLBAR_W + SCROLLBAR_PAD); // avoid scrollbar area
            int rowY1 = y + ROW_H;

            boolean hovered = mouseX >= rowX0 && mouseX <= rowX1 && mouseY >= rowY0 && mouseY <= rowY1;

            // subtle background on hover
            if (hovered) {
                context.fill(rowX0, rowY0 + 1, rowX1, rowY1 - 1, 0x33111111);
                hoveredDef = def;
                hoveredRowX0 = rowX0; hoveredRowY0 = rowY0; hoveredRowX1 = rowX1; hoveredRowY1 = rowY1;
            }

            // icon
            int iconX = rowLeft;
            int iconY = y + (ROW_H - ICON_BOX) / 2;

            context.fill(iconX, iconY, iconX + ICON_BOX, iconY + ICON_BOX, 0xFF111111);
            ItemStack icon = iconStack(def);
            if (!icon.isEmpty()) {
                context.drawItem(icon, iconX + 2, iconY + 2);
            }

            // text area
            int btnX = rowRight - BTN_W;
            int textX = iconX + ICON_BOX + 10;
            int textW = Math.max(120, btnX - textX - 10);

            int current = ClientPassiveSkills.getLevel(def.id);

            String name = (def.name == null || def.name.isBlank()) ? def.id : def.name;
            context.drawTextWithShadow(
                    textRenderer,
                    Text.literal(name + " [" + current + "/" + def.maxLevel + "]"),
                    textX,
                    y + 6,
                    0xFFFFFFAA
            );

            String descStr = (def.description == null || def.description.isBlank()) ? "Описание не задано." : def.description;
            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(descStr), textW);

            // сколько строк реально помещается в ROW_H
            int availableH = ROW_H - 24; // имя + отступы
            int maxLines = Math.max(1, availableH / DESC_LINE_H);
            int toDraw = Math.min(maxLines, lines.size());

            int descY = y + 20;
            for (int j = 0; j < toDraw; j++) {
                // если строк больше чем помещается — последнюю строку рисуем как "..."
                if (j == toDraw - 1 && lines.size() > toDraw) {
                    int dotsW = textRenderer.getWidth("...");
                    int maxW = Math.max(0, textW - dotsW);

                    // trimToWidth(String, int) возвращает String — безопасно
                    String trimmed = textRenderer.trimToWidth(descStr, maxW);
                    context.drawTextWithShadow(
                            textRenderer,
                            Text.literal(trimmed + "..."),
                            textX,
                            descY + j * DESC_LINE_H,
                            0xFFCCCCCC
                    );
                    break;
                } else {
                    context.drawTextWithShadow(textRenderer, lines.get(j), textX, descY + j * DESC_LINE_H, 0xFFCCCCCC);
                }
            }



            y += ROW_H;
        }

        // draw scrollbar (track + thumb)
        if (sc.maxScroll > 0) {
            ScrollbarGeom g = computeScrollbarGeom(sc, visibleSkills.size());

            // track
            context.fill(g.trackX, g.trackY, g.trackX + g.trackW, g.trackY + g.trackH, 0xFF2A2A2A);

            // thumb
            int thumbColor = (isMouseOverScrollbar(mouseX, mouseY) || draggingScrollbar) ? 0xFFB0B0B0 : 0xFF8A8A8A;
            context.fill(g.thumbX, g.thumbY, g.thumbX + g.thumbW, g.thumbY + g.thumbH, thumbColor);
        }

        // ---- Full description tooltip on hover (full description) ----
        if (hoveredDef != null) {
            int tooltipW = Math.min(260, panelW - 40);

            String titleStr = (hoveredDef.name == null || hoveredDef.name.isBlank()) ? hoveredDef.id : hoveredDef.name;
            String descStr = (hoveredDef.description == null || hoveredDef.description.isBlank())
                    ? "Описание не задано."
                    : hoveredDef.description;

            List<Text> tLines = new ArrayList<>();
            tLines.add(Text.literal(titleStr));
            tLines.add(Text.empty());
            tLines.addAll(wrapStringToTextLines(descStr, tooltipW));

            context.drawTooltip(textRenderer, tLines, mouseX, mouseY);
        }
    }

    private boolean isMouseOverScrollbar(int mouseX, int mouseY) {
        ScrollContext sc = computeScrollContext(visibleSkills == null ? 0 : visibleSkills.size());
        if (sc.maxScroll <= 0) return false;

        int x0 = sc.trackX;
        int y0 = sc.trackY;
        return mouseX >= x0 && mouseX <= x0 + SCROLLBAR_W && mouseY >= y0 && mouseY <= y0 + sc.trackH;
    }

    private ScrollContext computeScrollContext(int totalRows) {
        int panelW = Math.min(BASE_PANEL_W, this.width - 40);
        int panelH = Math.min(BASE_PANEL_H, this.height - 40);
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        int contentY = top + HEADER_H;
        int contentH = panelH - HEADER_H - 10;

        int visibleRows = Math.max(1, contentH / ROW_H);
        int maxScroll = Math.max(0, totalRows - visibleRows);

        int trackX = left + panelW - SCROLLBAR_PAD - SCROLLBAR_W;
        int trackY = contentY;
        int trackH = contentH;

        return new ScrollContext(left, top, panelW, panelH, contentY, contentH, visibleRows, maxScroll, trackX, trackY, trackH);
    }

    private ScrollbarGeom computeScrollbarGeom(ScrollContext sc, int totalRows) {
        int maxScroll = Math.max(1, sc.maxScroll);

        float proportion = (float) sc.visibleRows / (float) Math.max(sc.visibleRows, Math.max(1, totalRows));
        int thumbH = MathHelper.clamp(Math.round(sc.trackH * proportion), SCROLLBAR_MIN_THUMB_H, sc.trackH);

        int travel = Math.max(1, sc.trackH - thumbH);
        float t = (sc.maxScroll <= 0) ? 0f : ((float) scroll / (float) sc.maxScroll);
        int thumbY = sc.trackY + Math.round(travel * t);

        return new ScrollbarGeom(sc.trackX, sc.trackY, SCROLLBAR_W, sc.trackH, sc.trackX, thumbY, SCROLLBAR_W, thumbH);
    }

    private record ScrollContext(
            int left, int top, int panelW, int panelH,
            int contentY, int contentH,
            int visibleRows, int maxScroll,
            int trackX, int trackY, int trackH
    ) {}

    private record ScrollbarGeom(
            int trackX, int trackY, int trackW, int trackH,
            int thumbX, int thumbY, int thumbW, int thumbH
    ) {}

    private static ItemStack iconStack(PassiveSkillsConfig.PassiveSkillDef def) {
        if (def == null) return ItemStack.EMPTY;
        if (def.iconItem == null || def.iconItem.isBlank()) return ItemStack.EMPTY;

        Identifier id;
        try {
            id = Identifier.of(def.iconItem);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(id);
        if (item == null) return ItemStack.EMPTY;

        ItemStack st = new ItemStack(item);
        return st.isEmpty() ? ItemStack.EMPTY : st;
    }

    private static int costForNext(PassiveSkillsConfig.PassiveSkillDef def, int nextLevel) {
        if (def == null) return Integer.MAX_VALUE;
        int lv = Math.max(1, nextLevel);
        long cost = (long) def.costBase + (long) (lv - 1) * (long) def.costPerLevel;
        if (cost < 0) cost = Integer.MAX_VALUE;
        if (cost > Integer.MAX_VALUE) cost = Integer.MAX_VALUE;
        return (int) cost;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ====== Optional: keep navigation sane ======

    @Nullable
    @Override
    public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        return super.getNavigationPath(navigation);
    }

    @Override
    public ScreenRect getNavigationFocus() {
        return super.getNavigationFocus();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        return super.charTyped(input);
    }
}
