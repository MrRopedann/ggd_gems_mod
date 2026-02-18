package su.ggd.ggd_gems_mod.socket;

import net.minecraft.item.ItemStack;
import su.ggd.ggd_gems_mod.gem.GemRegistry;
import su.ggd.ggd_gems_mod.registry.ModDataComponents;
import su.ggd.ggd_gems_mod.targeting.GemTargeting;

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

    /**
     * Проверка, можно ли вставлять самоцвет данного типа в предмет (targets из конфига).
     */
    public static boolean canInsert(ItemStack target, String gemType) {
        if (target == null || target.isEmpty()) return false;
        if (gemType == null || gemType.isBlank()) return false;

        var def = GemRegistry.get(gemType);
        if (def == null) return false;

        return GemTargeting.canInsertGemInto(target, def);
    }

    /**
     * Добавляет/апгрейдит самоцвет в DataComponent слотов.
     *
     * Логика:
     * - если тип уже вставлен: можно апгрейдить только на следующий уровень
     * - если тип не вставлен: можно вставить только уровень 1
     * - maxLevel берём из GemRegistry (def.maxLevel, дефолт 10)
     *
     * ВАЖНО: targets check не делается здесь — используйте {@link #canInsert(ItemStack, String)} до addGem().
     */
    public static boolean addGem(ItemStack target, String gemType, int gemLevel) {
        if (gemType == null || gemType.isBlank()) return false;
        if (gemLevel <= 0) return false;

        String key = gemType.trim().toLowerCase(java.util.Locale.ROOT);

        int max = 10;
        var def = GemRegistry.get(key);
        if (def != null && def.maxLevel > 0) max = def.maxLevel;

        List<SocketedGem> current = new ArrayList<>(getSocketed(target));

        for (int i = 0; i < current.size(); i++) {
            SocketedGem existing = current.get(i);
            if (existing == null) continue;

            if (key.equals(existing.type())) {
                int expected = existing.level() + 1;
                if (existing.level() >= max) return false;
                if (gemLevel != expected) return false;

                current.set(i, new SocketedGem(key, expected));
                target.set(ModDataComponents.SOCKETED_GEMS, current);
                return true;
            }
        }

        if (gemLevel != 1) return false;
        if (current.size() >= MAX_SOCKETS) return false;

        current.add(new SocketedGem(key, 1));
        target.set(ModDataComponents.SOCKETED_GEMS, current);
        return true;
    }
}
