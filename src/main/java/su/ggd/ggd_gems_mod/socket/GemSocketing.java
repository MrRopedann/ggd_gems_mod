package su.ggd.ggd_gems_mod.socket;

import net.minecraft.item.ItemStack;
import su.ggd.ggd_gems_mod.registry.ModDataComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GemSocketing {
    private GemSocketing() {}

    public static final int MAX_SOCKETS = 2;

    public static List<SocketedGem> getSocketed(ItemStack stack) {
        if (ModDataComponents.SOCKETED_GEMS == null) return Collections.emptyList();
        List<SocketedGem> v = stack.get(ModDataComponents.SOCKETED_GEMS);
        return v == null ? Collections.emptyList() : v;
    }

    public static boolean hasFreeSocket(ItemStack stack) {
        return getSocketed(stack).size() < MAX_SOCKETS;
    }

    public static boolean addGem(ItemStack target, String gemType, int gemLevel) {
        if (gemType == null || gemType.isBlank()) return false;
        if (gemLevel <= 0) return false;

        // max level берём из конфига (дефолт 10)
        int max = 10;
        var cfg = su.ggd.ggd_gems_mod.config.GemsConfigManager.get();
        if (cfg != null && cfg.maxLevel > 0) max = cfg.maxLevel;

        List<SocketedGem> current = new ArrayList<>(getSocketed(target));

        // 1) Если такой тип уже есть — разрешаем ТОЛЬКО следующий уровень
        for (int i = 0; i < current.size(); i++) {
            SocketedGem existing = current.get(i);
            if (existing.type().equals(gemType)) {
                int expected = existing.level() + 1;

                // уже max — апгрейд невозможен
                if (existing.level() >= max) return false;

                // чтобы не терять ресурс: разрешаем только точный next-level
                if (gemLevel != expected) return false;

                current.set(i, new SocketedGem(gemType, expected));
                target.set(ModDataComponents.SOCKETED_GEMS, current);
                return true;
            }
        }

        // 2) Такого типа нет — разрешаем вставку ТОЛЬКО lvl1 (без перескоков)
        if (gemLevel != 1) return false;

        if (current.size() >= MAX_SOCKETS) return false;

        current.add(new SocketedGem(gemType, 1));
        target.set(ModDataComponents.SOCKETED_GEMS, current);
        return true;
    }


}
