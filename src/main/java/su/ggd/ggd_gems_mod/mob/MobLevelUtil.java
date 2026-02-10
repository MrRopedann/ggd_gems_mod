package su.ggd.ggd_gems_mod.mob;

import net.minecraft.entity.Entity;

import java.util.List;

/**
 * Уровень моба хранится через command tags сущности.
 * Command tags синхронизируются на клиент, поэтому уровень можно читать в HUD.
 */
public final class MobLevelUtil {
    private MobLevelUtil() {}

    public static final String TAG_PREFIX = "ggd_lvl=";

    public static int getLevel(Entity entity) {
        if (entity == null) return 1;

        for (String tag : entity.getCommandTags()) {
            if (tag == null) continue;
            if (!tag.startsWith(TAG_PREFIX)) continue;
            String v = tag.substring(TAG_PREFIX.length());
            try {
                int lvl = Integer.parseInt(v);
                return Math.max(1, lvl);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    public static boolean hasLevelTag(Entity entity) {
        if (entity == null) return false;
        for (String tag : entity.getCommandTags()) {
            if (tag != null && tag.startsWith(TAG_PREFIX)) return true;
        }
        return false;
    }

    public static void setLevel(Entity entity, int level) {
        if (entity == null) return;
        int lvl = Math.max(1, level);

        // убрать старые теги уровня
        List<String> copy = List.copyOf(entity.getCommandTags());
        for (String t : copy) {
            if (t != null && t.startsWith(TAG_PREFIX)) {
                entity.removeCommandTag(t);
            }
        }

        entity.addCommandTag(TAG_PREFIX + lvl);
    }
}
