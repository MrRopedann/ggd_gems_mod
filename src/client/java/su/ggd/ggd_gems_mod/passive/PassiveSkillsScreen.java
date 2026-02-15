package su.ggd.ggd_gems_mod.passive;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;

import java.util.*;

public class PassiveSkillsScreen extends Screen {

    // окно
    private static final int BASE_PANEL_W = 280;
    private static final int BASE_PANEL_H = 240;

    private static final int PAD_X = 12;

    private static final int ICON_BOX = 20;

    // кнопка апгрейда "+"
    private static final int UPGRADE_BTN_W = 22;
    private static final int BTN_H = 20;
    private static final int RIGHT_BTN_PAD = 6;

    // header
    private static final int TITLE_Y = 12;
    private static final int PLAYER_LV_Y = 26;

    // dropdown
    private static final int TAB_DD_Y = 46;
    private static final int TAB_DD_H = 18;
    private static final int TAB_DD_LIST_ROW_H = 18;
    private static final int TAB_DD_LIST_PAD = 2;
    private static final int TAB_DD_MAX_VISIBLE = 7;
    private static final int TAB_DD_GAP_BELOW = 6; // зазор между списком и контентом

    // строки
    private static final int ROW_H = 32;

    // scrollbar
    private static final int SCROLLBAR_W = 6;
    private static final int SCROLLBAR_PAD = 6;
    private static final int SCROLLBAR_MIN_THUMB_H = 14;

    private final Map<String, ButtonWidget> upgradeButtons = new HashMap<>();

    // dropdown state
    private ButtonWidget tabDropdownBtn;
    private boolean tabDropdownOpen = false;
    private int tabDropdownScroll = 0;

    // bounds
    private int tabDdX, tabDdY, tabDdW, tabDdH;

    // list bounds (computed dynamically)
    private int tabListX, tabListY, tabListW, tabListH;

    private List<TabEntry> tabs = Collections.emptyList();

    // costs
    private final Map<String, Integer> visibleCosts = new HashMap<>();
    private final Map<String, Boolean> visibleCanUpgrade = new HashMap<>();

    // ui message
    private Text uiMessage = null;
    private int uiMessageTicks = 0;

    // selected tab
    private String selectedTabId = "all";

    // visible skills for selected tab
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
        visibleCosts.clear();
        visibleCanUpgrade.clear();

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null) return;

        this.tabs = buildTabs(cfg);
        if (tabs.stream().noneMatch(t -> Objects.equals(t.id, selectedTabId))) {
            selectedTabId = "all";
        }

        visibleSkills = computeVisibleSkills(cfg);

        int panelW = Math.min(BASE_PANEL_W, this.width - 40);
        int panelH = Math.min(BASE_PANEL_H, this.height - 40);
        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        // dropdown button bounds
        tabDdX = left + PAD_X;
        tabDdY = top + TAB_DD_Y;
        tabDdW = panelW - PAD_X * 2;
        tabDdH = TAB_DD_H;

        tabDropdownBtn = ButtonWidget.builder(
                getSelectedTabLabel(),
                b -> {
                    tabDropdownOpen = !tabDropdownOpen;
                    // пересобираем, т.к. меняется headerH и расположение контента
                    this.clearChildren();
                    this.init();
                }
        ).dimensions(tabDdX, tabDdY, tabDdW, tabDdH).build();

        addDrawableChild(tabDropdownBtn);

        // list bounds (computed based on current open state)
        recomputeTabListBounds();

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
            int btnX = rowRight - RIGHT_BTN_PAD - UPGRADE_BTN_W;
            int btnY = y + (ROW_H - BTN_H) / 2;

            ButtonWidget btn = ButtonWidget.builder(
                    Text.literal("+"),
                    b -> {
                        ClientPlayNetworking.send(new PassiveSkillsNet.UpgradePassiveSkillPayload(def.id));
                        showUiMessage(Text.literal("Запрос улучшения отправлен..."), 40);
                    }
            ).dimensions(btnX, btnY, UPGRADE_BTN_W, BTN_H).build();

            addDrawableChild(btn);
            upgradeButtons.put(def.id, btn);

            y += ROW_H;
        }

        refreshButtons();
    }

    private void recomputeTabListBounds() {
        tabListX = tabDdX;
        tabListY = tabDdY + tabDdH + 2;
        tabListW = tabDdW;

        int visible = Math.min(TAB_DD_MAX_VISIBLE, Math.max(1, tabs.size()));
        tabListH = visible * TAB_DD_LIST_ROW_H + TAB_DD_LIST_PAD * 2;

        tabDropdownScroll = MathHelper.clamp(tabDropdownScroll, 0, Math.max(0, tabs.size() - TAB_DD_MAX_VISIBLE));
    }

    private List<TabEntry> buildTabs(PassiveSkillsConfig cfg) {
        List<TabEntry> out = new ArrayList<>();
        out.add(new TabEntry("all", "Все"));

        if (cfg.tabs != null) {
            for (var t : cfg.tabs) {
                if (t == null || t.id == null || t.id.isBlank()) continue;
                String name = (t.name == null || t.name.isBlank()) ? t.id : t.name;
                out.add(new TabEntry(t.id, name));
            }
        }

        tabDropdownScroll = MathHelper.clamp(tabDropdownScroll, 0, Math.max(0, out.size() - TAB_DD_MAX_VISIBLE));
        return out;
    }

    private Text getSelectedTabLabel() {
        String name = selectedTabId;
        for (var t : tabs) {
            if (Objects.equals(t.id, selectedTabId)) {
                name = t.name;
                break;
            }
        }
        return Text.literal("Вкладка: " + name + " \u25BE");
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
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (tabDropdownOpen && isMouseOverTabList(mx, my) && tabs.size() > TAB_DD_MAX_VISIBLE) {
            int delta = (int) Math.signum(verticalAmount);
            if (delta != 0) {
                int max = Math.max(0, tabs.size() - TAB_DD_MAX_VISIBLE);
                tabDropdownScroll = MathHelper.clamp(tabDropdownScroll - delta, 0, max);
                return true;
            }
        }

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

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();

        // dropdown list interactions first
        if (click.button() == 0 && tabDropdownOpen) {
            if (isMouseOverTabList(mx, my)) {
                int idx = (my - (tabListY + TAB_DD_LIST_PAD)) / TAB_DD_LIST_ROW_H;
                int actual = tabDropdownScroll + idx;
                if (idx >= 0 && actual >= 0 && actual < tabs.size()) {
                    TabEntry chosen = tabs.get(actual);
                    selectedTabId = chosen.id;
                    tabDropdownOpen = false;
                    scroll = 0;
                    this.clearChildren();
                    this.init();
                    return true;
                }
                return true;
            } else if (!isMouseOverTabButton(mx, my)) {
                tabDropdownOpen = false;
                this.clearChildren();
                this.init();
                return true;
            }
        }

        // scrollbar interactions
        if (click.button() == 0 && isMouseOverScrollbar(mx, my)) {
            ScrollContext sc = computeScrollContext(visibleSkills == null ? 0 : visibleSkills.size());
            if (sc.maxScroll > 0) {
                ScrollbarGeom g = computeScrollbarGeom(sc, visibleSkills.size());

                if (my >= g.thumbY && my <= g.thumbY + g.thumbH) {
                    draggingScrollbar = true;
                    dragStartMouseY = my;
                    dragStartScroll = scroll;
                    return true;
                } else {
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

        if (tabDropdownBtn != null) tabDropdownBtn.setMessage(getSelectedTabLabel());

        refreshButtons();
    }

    private void refreshButtons() {
        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg == null || mc.player == null) return;

        int playerLevel = mc.player.experienceLevel;

        for (PassiveSkillsConfig.PassiveSkillDef def : visibleSkills) {
            if (def == null) continue;

            ButtonWidget btn = upgradeButtons.get(def.id);
            if (btn == null) continue;

            int cur = ClientPassiveSkills.getLevel(def.id);
            int next = cur + 1;
            int cost = costForNext(def, next);

            boolean canUpgrade = next <= def.maxLevel && playerLevel >= cost;
            btn.active = canUpgrade;

            visibleCosts.put(def.id, cost);
            visibleCanUpgrade.put(def.id, canUpgrade);

            btn.setTooltip(Tooltip.of(Text.literal("Апгрейд: " + cost + " ур. (до " + next + "/" + def.maxLevel + ")")));
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

        super.render(context, mouseX, mouseY, delta);

        // header text
        context.drawTextWithShadow(textRenderer, this.title, left + 12, top + TITLE_Y, 0xFFFFFFFF);

        MinecraftClient mc = MinecraftClient.getInstance();
        int playerLevel = mc.player == null ? 0 : mc.player.experienceLevel;
        context.drawTextWithShadow(textRenderer, Text.literal("Уровни игрока: " + playerLevel), left + 12, top + PLAYER_LV_Y, 0xFFAAAAAA);

        if (uiMessage != null) {
            int msgW = textRenderer.getWidth(uiMessage);
            int msgX = left + (panelW - msgW) / 2;
            int msgY = top + 36;
            context.drawTextWithShadow(textRenderer, uiMessage, msgX, msgY, 0xFFFFFF55);
        }

        // render dropdown list above content area
        recomputeTabListBounds();
        TabEntry hoveredTab = null;

        if (tabDropdownOpen) {
            int x0 = tabListX;
            int y0 = tabListY;
            int x1 = tabListX + tabListW;
            int y1 = tabListY + tabListH;

            context.fill(x0, y0, x1, y1, 0xEE1A1A1A);
            context.fill(x0, y0, x1, y0 + 1, 0xFF777777);
            context.fill(x0, y1 - 1, x1, y1, 0xFF777777);
            context.fill(x0, y0, x0 + 1, y1, 0xFF777777);
            context.fill(x1 - 1, y0, x1, y1, 0xFF777777);

            int visible = Math.min(TAB_DD_MAX_VISIBLE, Math.max(1, tabs.size()));
            int start = tabDropdownScroll;
            int end = Math.min(tabs.size(), start + visible);

            int cy = y0 + TAB_DD_LIST_PAD;

            for (int i = start; i < end; i++) {
                TabEntry te = tabs.get(i);
                boolean hovered = mouseX >= x0 && mouseX <= x1 && mouseY >= cy && mouseY < cy + TAB_DD_LIST_ROW_H;

                if (hovered) {
                    context.fill(x0 + 1, cy, x1 - 1, cy + TAB_DD_LIST_ROW_H, 0x33222222);
                    hoveredTab = te;
                }

                boolean selected = Objects.equals(te.id, selectedTabId);
                String prefix = selected ? "• " : "  ";

                String full = prefix + te.name;
                String shown = textRenderer.trimToWidth(full, tabListW - 10);

                int ty = cy + (TAB_DD_LIST_ROW_H - textRenderer.fontHeight) / 2;
                int color = selected ? 0xFFFFFFAA : 0xFFDDDDDD;
                context.drawTextWithShadow(textRenderer, Text.literal(shown), x0 + 6, ty, color);

                cy += TAB_DD_LIST_ROW_H;
            }
        }

        // content area (starts below dropdown if opened)
        if (visibleSkills == null) return;

        ScrollContext sc = computeScrollContext(visibleSkills.size());
        scroll = MathHelper.clamp(scroll, 0, sc.maxScroll);

        int start = scroll;
        int end = Math.min(visibleSkills.size(), start + sc.visibleRows);

        int y = sc.contentY;

        PassiveSkillsConfig.PassiveSkillDef hoveredDef = null;

        for (int idx = start; idx < end; idx++) {
            PassiveSkillsConfig.PassiveSkillDef def = visibleSkills.get(idx);
            if (def == null) {
                y += ROW_H;
                continue;
            }

            int rowLeft = left + PAD_X;
            int rowRight = left + panelW - PAD_X;

            int rowX0 = rowLeft;
            int rowY0 = y;
            int rowX1 = rowRight - (SCROLLBAR_W + SCROLLBAR_PAD + RIGHT_BTN_PAD);
            int rowY1 = y + ROW_H;

            boolean hovered = mouseX >= rowX0 && mouseX <= rowX1 && mouseY >= rowY0 && mouseY <= rowY1;

            if (hovered) {
                context.fill(rowX0, rowY0 + 1, rowX1, rowY1 - 1, 0x33111111);
                hoveredDef = def;
            }

            context.fill(rowX0, rowY1 - 1, rowX1, rowY1, 0x22111111);

            int iconX = rowLeft;
            int iconY = y + (ROW_H - ICON_BOX) / 2;

            context.fill(iconX, iconY, iconX + ICON_BOX, iconY + ICON_BOX, 0xFF111111);
            ItemStack icon = iconStack(def);
            if (!icon.isEmpty()) context.drawItem(icon, iconX + 2, iconY + 2);

            int btnX = rowRight - RIGHT_BTN_PAD - UPGRADE_BTN_W;

            Integer cost = visibleCosts.get(def.id);
            boolean canUp = Boolean.TRUE.equals(visibleCanUpgrade.get(def.id));

            if (cost != null) {
                String costStr = String.valueOf(cost);
                int costW = textRenderer.getWidth(costStr);

                int costX = btnX - 6 - costW;
                int costY = y + (ROW_H - textRenderer.fontHeight) / 2;

                int costColor = canUp ? 0xFFAAAAAA : 0xFF777777;
                context.drawTextWithShadow(textRenderer, Text.literal(costStr), costX, costY, costColor);
            }

            int current = ClientPassiveSkills.getLevel(def.id);
            String name = (def.name == null || def.name.isBlank()) ? def.id : def.name;

            int safeRight = (cost != null)
                    ? (btnX - 6 - textRenderer.getWidth(String.valueOf(cost)) - 10)
                    : (btnX - 10);

            int textX = iconX + ICON_BOX + 10;
            int maxNameW = Math.max(10, safeRight - textX);
            String shownName = textRenderer.trimToWidth(name + " [" + current + "/" + def.maxLevel + "]", maxNameW);

            int nameY = y + (ROW_H - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, Text.literal(shownName), textX, nameY, 0xFFFFFFAA);

            y += ROW_H;
        }

        if (sc.maxScroll > 0) {
            ScrollbarGeom g = computeScrollbarGeom(sc, visibleSkills.size());
            context.fill(g.trackX, g.trackY, g.trackX + g.trackW, g.trackY + g.trackH, 0xFF2A2A2A);
            int thumbColor = (isMouseOverScrollbar(mouseX, mouseY) || draggingScrollbar) ? 0xFFB0B0B0 : 0xFF8A8A8A;
            context.fill(g.thumbX, g.thumbY, g.thumbX + g.thumbW, g.thumbY + g.thumbH, thumbColor);
        }

        if (hoveredTab != null) {
            context.drawTooltip(textRenderer, List.of(Text.literal(hoveredTab.name)), mouseX, mouseY);
        }

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

    private boolean isMouseOverTabButton(int mouseX, int mouseY) {
        return mouseX >= tabDdX && mouseX <= tabDdX + tabDdW && mouseY >= tabDdY && mouseY <= tabDdY + tabDdH;
    }

    private boolean isMouseOverTabList(int mouseX, int mouseY) {
        int x0 = tabListX;
        int y0 = tabListY;
        int x1 = tabListX + tabListW;
        int y1 = tabListY + tabListH;
        return mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1;
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

        // базовый header: до кнопки dropdown
        int headerH = TAB_DD_Y + TAB_DD_H + 10;

        // если dropdown открыт — контент должен начинаться ниже списка
        if (tabDropdownOpen) {
            recomputeTabListBounds();
            headerH = (TAB_DD_Y + TAB_DD_H) + 2 + tabListH + TAB_DD_GAP_BELOW;
        }

        int contentY = top + headerH;
        int contentH = panelH - headerH - 10;

        int visibleRows = Math.max(1, contentH / ROW_H);
        int maxScroll = Math.max(0, totalRows - visibleRows);

        int trackX = left + panelW - SCROLLBAR_PAD - SCROLLBAR_W;
        int trackY = contentY;
        int trackH = contentH;

        return new ScrollContext(left, top, panelW, panelH, contentY, contentH, visibleRows, maxScroll, trackX, trackY, trackH);
    }

    private ScrollbarGeom computeScrollbarGeom(ScrollContext sc, int totalRows) {
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

    private record TabEntry(String id, String name) {}

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
