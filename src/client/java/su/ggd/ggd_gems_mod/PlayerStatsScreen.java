package su.ggd.ggd_gems_mod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlayerStatsScreen extends Screen {

    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 250;

    private static final int HEADER_H = 34;
    private static final int FOOTER_H = 18;
    private static final int ROW_H = 14;

    private int scroll = 0;

    public PlayerStatsScreen() {
        super(Text.literal("Статы персонажа"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // прокрутка списка
        int delta = (int) Math.signum(verticalAmount);
        if (delta != 0) {
            scroll -= delta; // колесо вверх => меньше scroll
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        var mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null) return;

        int px = (this.width - PANEL_WIDTH) / 2;
        int py = (this.height - PANEL_HEIGHT) / 2;

        // затемнение
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        // панель + рамка
        ctx.fill(px, py, px + PANEL_WIDTH, py + PANEL_HEIGHT, 0xCC1E1E1E);
        drawBorder(ctx, px, py, PANEL_WIDTH, PANEL_HEIGHT, 0xFFAA8833);

        // заголовок
        ctx.drawTextWithShadow(textRenderer, "СТАТЫ ПЕРСОНАЖА", px + 12, py + 10, 0xFFFFD700);
        ctx.fill(px + 12, py + 26, px + PANEL_WIDTH - 12, py + 27, 0xFF555555);

        ItemStack mainHand = p.getMainHandStack();

        // собираем строки
        List<StatLine> lines = buildLines();
        int contentH = PANEL_HEIGHT - HEADER_H - FOOTER_H;
        int visibleRows = Math.max(1, contentH / ROW_H);
        int maxScroll = Math.max(0, lines.size() - visibleRows);
        if (scroll < 0) scroll = 0;
        if (scroll > maxScroll) scroll = maxScroll;

        int x = px + 14;
        int y = py + HEADER_H;

        // рисуем видимый диапазон
        int start = scroll;
        int end = Math.min(lines.size(), start + visibleRows);

        for (int i = start; i < end; i++) {
            StatLine sl = lines.get(i);
            Stat s = sl.mainhand
                    ? readAttrWithMainHand(p, mainHand, sl.attrPath)
                    : readAttr(p, sl.attrPath);

            drawStatLine(ctx, x, y, sl.icon, sl.nameRu, s, sl.color);
            y += ROW_H;
        }

        // футер
        String footer = (maxScroll > 0)
                ? String.format(Locale.ROOT, "Колесо — прокрутка  (%d/%d)", scroll + 1, maxScroll + 1)
                : "ESC — закрыть";
        ctx.drawTextWithShadow(textRenderer, footer, px + 12, py + PANEL_HEIGHT - 14, 0xFFAAAAAA);
    }

    /* -------------------- Lines -------------------- */

    private static List<StatLine> buildLines() {
        List<StatLine> list = new ArrayList<>();

        // Боевые (с MAINHAND)
        list.add(new StatLine("minecraft:attack_damage", "Урон атаки", "⚔", 0xFFFF5555, true));
        list.add(new StatLine("minecraft:attack_speed", "Скорость атаки", "⏱", 0xFFFFAA55, true));
        list.add(new StatLine("minecraft:attack_knockback", "Отбрасывание", "➶", 0xFFFFAA55, true));
        list.add(new StatLine("minecraft:sweeping_damage_ratio", "Размашистый урон", "〰", 0xFFFFAA55, true));

        // Защита/сопротивления (экипировка)
        list.add(new StatLine("minecraft:armor", "Броня", "🛡", 0xFF55AAFF, false));
        list.add(new StatLine("minecraft:knockback_resistance", "Сопр. отбрасыванию", "⛨", 0xFF55AAFF, false));
        list.add(new StatLine("minecraft:explosion_knockback_resistance", "Сопр. взрывам", "💥", 0xFF55AAFF, false));

        // Выживание
        list.add(new StatLine("minecraft:max_health", "Макс. здоровье", "❤", 0xFF55FF55, false));
        list.add(new StatLine("minecraft:max_absorption", "Макс. поглощение", "💛", 0xFF55FF55, false));
        list.add(new StatLine("minecraft:luck", "Удача", "🍀", 0xFF55FF55, false));
        list.add(new StatLine("minecraft:oxygen_bonus", "Кислород", "🫧", 0xFF55FF55, false));
        list.add(new StatLine("minecraft:fall_damage_multiplier", "Урон от падения", "⬇", 0xFF55FF55, false));

        // Движение
        list.add(new StatLine("minecraft:movement_speed", "Скорость", "➜", 0xFFFFFFAA, false));
        list.add(new StatLine("minecraft:jump_strength", "Сила прыжка", "⤒", 0xFFFFFFAA, false));

        // Инструменты (с MAINHAND)
        list.add(new StatLine("minecraft:block_break_speed", "Скорость ломания", "⛏", 0xFFAAAAFF, true));
        list.add(new StatLine("minecraft:mining_efficiency", "Эффективность добычи", "⚙", 0xFFAAAAFF, true));
        list.add(new StatLine("minecraft:submerged_mining_speed", "Добыча под водой", "🌊", 0xFFAAAAFF, true));

        return list;
    }

    private record StatLine(String attrPath, String nameRu, String icon, int color, boolean mainhand) {}

    /* -------------------- Stats math -------------------- */

    private static final class Stat {
        final double base;
        final double bonus;
        final double total;

        private Stat(double base, double bonus, double total) {
            this.base = base;
            this.bonus = bonus;
            this.total = total;
        }
    }

    // Для статов, которые напрямую на игроке (включая броню/ботинки и т.п.)
    private static Stat readAttr(net.minecraft.entity.player.PlayerEntity p, String attrId) {
        RegistryEntry<EntityAttribute> entry = attrEntry(attrId);
        if (entry == null) return new Stat(0, 0, 0);

        EntityAttributeInstance inst = p.getAttributeInstance(entry);
        if (inst == null) return new Stat(0, 0, 0);

        double base = inst.getBaseValue();
        double total = inst.getValue();
        return new Stat(base, total - base, total);
    }

    // Для статов, где итог зависит от предмета в MAINHAND
    private static Stat readAttrWithMainHand(net.minecraft.entity.player.PlayerEntity p, ItemStack mainHand, String attrId) {
        Stat baseStat = readAttr(p, attrId);

        if (mainHand == null || mainHand.isEmpty()) return baseStat;

        RegistryEntry<EntityAttribute> attr = attrEntry(attrId);
        if (attr == null) return baseStat;

        AttributeModifiersComponent mods = mainHand.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (mods == null) return baseStat;

        double addValue = 0.0;
        double addMulBase = 0.0;
        double addMulTotal = 0.0;

        for (AttributeModifiersComponent.Entry e : mods.modifiers()) {
            if (!e.attribute().equals(attr)) continue;
            if (e.slot() != AttributeModifierSlot.MAINHAND) continue;

            EntityAttributeModifier m = e.modifier();
            double v = m.value();

            EntityAttributeModifier.Operation op = m.operation();
            if (op == EntityAttributeModifier.Operation.ADD_VALUE) {
                addValue += v;
            } else if (op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                addMulBase += v;
            } else if (op == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                addMulTotal += v;
            }
        }

        double base = baseStat.base;
        double total = base + addValue;
        total += base * addMulBase;
        total *= (1.0 + addMulTotal);

        return new Stat(base, total - base, total);
    }

    private static RegistryEntry<EntityAttribute> attrEntry(String idOrPath) {
        Identifier id = Identifier.tryParse(idOrPath);
        if (id == null) {
            id = Identifier.of("minecraft", idOrPath);
        }
        EntityAttribute attr = Registries.ATTRIBUTE.get(id);
        if (attr == null) return null;
        return Registries.ATTRIBUTE.getEntry(attr);
    }

    /* -------------------- Drawing -------------------- */

    private void drawStatLine(DrawContext ctx, int x, int y, String icon, String name, Stat s, int color) {
        String line;
        if (Math.abs(s.bonus) > 1e-9) {
            line = String.format(Locale.US, "%s %s: %s (%s)", icon, name, fmt2(s.total), fmtSigned2(s.bonus));
        } else {
            line = String.format(Locale.US, "%s %s: %s", icon, name, fmt2(s.total));
        }
        ctx.drawTextWithShadow(textRenderer, line, x, y, color);
    }

    private static String fmt2(double v) {
        // компактно: без лишних нулей
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.format(Locale.US, "%.0f", v);
        return String.format(Locale.US, "%.3f", v);
    }

    private static String fmtSigned2(double v) {
        String s = fmt2(Math.abs(v));
        return (v >= 0) ? ("+" + s) : ("-" + s);
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);                 // top
        ctx.fill(x, y + h - 1, x + w, y + h, color);         // bottom
        ctx.fill(x, y, x + 1, y + h, color);                 // left
        ctx.fill(x + w - 1, y, x + w, y + h, color);         // right
    }
}
