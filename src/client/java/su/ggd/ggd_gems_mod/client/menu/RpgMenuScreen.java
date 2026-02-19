package su.ggd.ggd_gems_mod.client.menu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.currency.client.CurrencyClientState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Единое окно для RPG-систем мода: статы, пассивки, квесты и будущие вкладки.
 */
public final class RpgMenuScreen extends Screen {

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;

    private static final int NAV_W = 110;
    private static final int PAD = 10;
    private static final int NAV_ROW_H = 26;

    // header
    private static final int HEADER_H = 34;
    private static final int HEAD_SIZE = 24;

    // nav scrolling
    private int navScrollIndex = 0;
    private int visibleNavRows = 0;
    private int navMaxScroll = 0;

    private final String initialPageId;

    private String selectedPageId;
    private List<RpgMenuPages.PageDef> pages;

    private final Map<String, Screen> pageScreens = new HashMap<>();
    private final Map<String, ButtonWidget> pageButtons = new HashMap<>();

    // layout
    private int panelX, panelY, panelW, panelH;
    private int headerX, headerY, headerW, headerH;
    private int navX, navY, navW, navH;
    private int contentX, contentY, contentW, contentH;

    public RpgMenuScreen(String initialPageId) {
        super(Text.literal("Меню"));
        this.initialPageId = initialPageId;
        this.selectedPageId = initialPageId;
    }

    public static void open(MinecraftClient client, String pageId) {
        if (client == null) return;
        client.setScreen(new RpgMenuScreen(pageId));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        pages = RpgMenuPages.all();
        if (pages.isEmpty()) {
            selectedPageId = null;
        } else {
            if (selectedPageId == null || RpgMenuPages.find(selectedPageId) == null) {
                selectedPageId = initialPageId;
            }
            if (selectedPageId == null || RpgMenuPages.find(selectedPageId) == null) {
                selectedPageId = pages.get(0).id();
            }
        }

        computeLayout();
        rebuildNavButtons();
        ensureActivePageInit();
    }

    private void computeLayout() {
        panelW = Math.min(PANEL_W, this.width - 40);
        panelH = Math.min(PANEL_H, this.height - 40);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        headerX = panelX + PAD;
        headerY = panelY + PAD;
        headerW = panelW - PAD * 2;
        headerH = HEADER_H;

        navX = panelX + PAD;
        navY = panelY + PAD + HEADER_H + PAD / 2;
        navW = Math.min(NAV_W, panelW - PAD * 2);
        navH = panelY + panelH - PAD - navY;

        contentX = navX + navW + PAD;
        contentY = navY;
        contentW = panelX + panelW - PAD - contentX;
        contentH = panelY + panelH - PAD - contentY;

        visibleNavRows = Math.max(1, navH / NAV_ROW_H);
        navMaxScroll = Math.max(0, (pages == null ? 0 : pages.size()) - visibleNavRows);
        navScrollIndex = clampInt(navScrollIndex, 0, navMaxScroll);
    }

    private void rebuildNavButtons() {
        // удалить только старые кнопки вкладок
        for (ButtonWidget b : pageButtons.values()) {
            if (b != null) this.remove(b);
        }
        this.pageButtons.clear();

        if (pages == null || pages.isEmpty()) return;

        int start = clampInt(navScrollIndex, 0, navMaxScroll);
        int end = Math.min(pages.size(), start + visibleNavRows);

        int y = navY;
        for (int i = start; i < end; i++) {
            RpgMenuPages.PageDef p = pages.get(i);
            if (p == null) continue;

            ButtonWidget btn = ButtonWidget.builder(p.title(), b -> {
                selectedPageId = p.id();
                ensureActivePageInit();
            }).dimensions(navX, y, navW, 20).build();

            addDrawableChild(btn);
            pageButtons.put(p.id(), btn);

            y += NAV_ROW_H;
        }
    }

    private Screen getOrCreatePageScreen(String id) {
        if (id == null) return null;
        Screen existing = pageScreens.get(id);
        if (existing != null) return existing;

        RpgMenuPages.PageDef def = RpgMenuPages.find(id);
        if (def == null) return null;

        Screen created = def.factory().get();
        pageScreens.put(id, created);
        return created;
    }

    private void ensureActivePageInit() {
        Screen s = getOrCreatePageScreen(selectedPageId);
        if (s == null) return;

        // 1.21.11: init(int, int)
        s.init(contentW, contentH);
    }

    private boolean isInContent(double x, double y) {
        return x >= contentX && x < contentX + contentW && y >= contentY && y < contentY + contentH;
    }

    private boolean isInNav(double x, double y) {
        return x >= navX && x < navX + navW && y >= navY && y < navY + navH;
    }

    @Override
    public void tick() {
        super.tick();
        Screen s = getOrCreatePageScreen(selectedPageId);
        if (s != null) s.tick();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // overlay
        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        // panel
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC0F0F0F);
        // border
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF777777);
        ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF777777);
        ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF777777);
        ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF777777);

        // header bg
        ctx.fill(headerX, headerY, headerX + headerW, headerY + headerH, 0x55222222);

        // nav background
        ctx.fill(navX - 2, navY - 2, navX + navW + 2, navY + navH + 2, 0x551A1A1A);

        // content background
        ctx.fill(contentX - 2, contentY - 2, contentX + contentW + 2, contentY + contentH + 2, 0x551A1A1A);

        drawHeader(ctx);

        if (navMaxScroll > 0) {
            if (navScrollIndex > 0) {
                ctx.drawText(this.textRenderer, Text.literal("▲"), navX + navW - 10, navY - 2, 0xFFFFFFFF, false);
            }
            if (navScrollIndex < navMaxScroll) {
                ctx.drawText(this.textRenderer, Text.literal("▼"), navX + navW - 10, navY + navH - 10, 0xFFFFFFFF, false);
            }
        }

        // кнопки/виджеты
        super.render(ctx, mouseX, mouseY, delta);

        // highlight selected tab
        for (var e : pageButtons.entrySet()) {
            if (!e.getKey().equals(selectedPageId)) continue;
            ButtonWidget b = e.getValue();
            if (b == null) continue;
            ctx.fill(b.getX() - 1, b.getY() - 1, b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 1, 0x40FFFFFF);
            break;
        }

        // render active page inside content rect
        Screen page = getOrCreatePageScreen(selectedPageId);
        if (page != null) {
            int relX = mouseX - contentX;
            int relY = mouseY - contentY;

            ctx.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

            var matrices = ctx.getMatrices();
            matrices.pushMatrix();
            matrices.translate(contentX, contentY);

            page.render(ctx, relX, relY, delta);

            matrices.popMatrix();
            ctx.disableScissor();
        }

        // icons on nav buttons
        if (pages != null && !pages.isEmpty()) {
            for (RpgMenuPages.PageDef p : pages) {
                if (p == null) continue;

                ButtonWidget b = pageButtons.get(p.id());
                if (b == null) continue;

                ItemStack icon = p.icon();
                if (icon == null || icon.isEmpty()) continue;

                ctx.drawItem(icon, b.getX() + 3, b.getY() + 2);
            }
        }
    }

    private void drawHeader(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int headX = headerX + 6;
        int headY = headerY + (headerH - HEAD_SIZE) / 2;

        // Иконка игрока: PLAYER_HEAD (предмет). Это компилируется везде.
        drawScaledItem(ctx, new ItemStack(Items.PLAYER_HEAD), headX, headY, HEAD_SIZE);

        int textX = headX + HEAD_SIZE + 8;

        String name = mc.player.getName().getString();
        int level = mc.player.experienceLevel;

        long money = CurrencyClientState.get();

        ItemStack moneyIcon = getPiastreIconStack();

        // Name
        ctx.drawText(this.textRenderer, Text.literal(name), textX, headerY + 6, 0xFFFFFFFF, false);

        // Level
        ctx.drawText(this.textRenderer, Text.literal("Уровень: " + level), textX, headerY + 6 + 12, 0xFFDDDDDD, false);

        // Money (right)
        int rightX = headerX + headerW - 6;
        String moneyText = String.valueOf(money);
        int moneyTextW = this.textRenderer.getWidth(moneyText);

        int iconX = rightX - moneyTextW - 2 - 16;
        int iconY = headerY + (headerH - 16) / 2;

        ctx.drawItem(moneyIcon, iconX, iconY);
        ctx.drawText(this.textRenderer, Text.literal(moneyText), iconX + 18, headerY + 6 + 12, 0xFFF0E68C, false);
    }

    private void drawScaledItem(DrawContext ctx, ItemStack stack, int x, int y, int size) {
        float scale = size / 16.0f;
        var matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        ctx.drawItem(stack, 0, 0);
        matrices.popMatrix();
    }

    private static ItemStack getPiastreIconStack() {
        Item it = Registries.ITEM.get(Identifier.of("ggd_gems_mod", "piastre"));
        if (it == null || it == Items.AIR) return new ItemStack(Items.GOLD_NUGGET);
        return new ItemStack(it);
    }

    private static int getPiastreCount(MinecraftClient mc) {
        if (mc.player == null) return 0;

        Item piastre = Registries.ITEM.get(Identifier.of("ggd_gems_mod", "piastre"));
        if (piastre == null || piastre == Items.AIR) return 0;

        PlayerInventory inv = mc.player.getInventory();
        int sum = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (!st.isEmpty() && st.getItem() == piastre) {
                sum += st.getCount();
            }
        }
        return sum;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 1) если курсор над навигацией — скроллим вкладки
        if (pages != null && !pages.isEmpty() && isInNav(mouseX, mouseY) && navMaxScroll > 0) {
            int dir = verticalAmount > 0 ? -1 : (verticalAmount < 0 ? 1 : 0);
            if (dir != 0) {
                int next = clampInt(navScrollIndex + dir, 0, navMaxScroll);
                if (next != navScrollIndex) {
                    navScrollIndex = next;
                    rebuildNavButtons();
                }
                return true;
            }
        }

        // 2) иначе — прокидываем в контент
        Screen page = getOrCreatePageScreen(selectedPageId);
        if (page != null && isInContent(mouseX, mouseY)) {
            return page.mouseScrolled(mouseX - contentX, mouseY - contentY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Screen page = getOrCreatePageScreen(selectedPageId);
        if (page != null && isInContent(click.x(), click.y())) {
            Click shifted = new Click(
                    click.x() - contentX,
                    click.y() - contentY,
                    click.buttonInfo()
            );
            if (page.mouseClicked(shifted, doubled)) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        Screen page = getOrCreatePageScreen(selectedPageId);
        if (page != null && isInContent(click.x(), click.y())) {
            Click shifted = new Click(
                    click.x() - contentX,
                    click.y() - contentY,
                    click.buttonInfo()
            );
            if (page.mouseReleased(shifted)) return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        Screen page = getOrCreatePageScreen(selectedPageId);
        if (page != null && isInContent(click.x(), click.y())) {
            Click shifted = new Click(
                    click.x() - contentX,
                    click.y() - contentY,
                    click.buttonInfo()
            );
            if (page.mouseDragged(shifted, offsetX, offsetY)) return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        Screen page = getOrCreatePageScreen(selectedPageId);
        if (page != null && page.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        Screen page = getOrCreatePageScreen(selectedPageId);
        if (page != null && page.charTyped(input)) return true;
        return super.charTyped(input);
    }

    private static int clampInt(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
