package su.ggd.ggd_gems_mod.client.menu;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Внутренний реестр вкладок единого RPG-окна.
 *
 * Не является Fabric-регистром, поэтому может заполняться на клиенте в init-планах.
 */
public final class RpgMenuPages {
    private RpgMenuPages() {}

    public record PageDef(String id, Text title, ItemStack icon, Supplier<? extends Screen> factory) {}

    private static final List<PageDef> PAGES = new ArrayList<>();

    public static void register(String id, Text title, ItemStack icon, Supplier<? extends Screen> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(factory, "factory");

        // replace if exists (id is the key)
        for (int i = 0; i < PAGES.size(); i++) {
            if (PAGES.get(i).id.equals(id)) {
                PAGES.set(i, new PageDef(id, title, icon, factory));
                return;
            }
        }
        PAGES.add(new PageDef(id, title, icon, factory));
    }

    public static List<PageDef> all() {
        return Collections.unmodifiableList(PAGES);
    }

    public static PageDef find(String id) {
        if (id == null) return null;
        for (PageDef p : PAGES) {
            if (p != null && id.equals(p.id)) return p;
        }
        return null;
    }
}
