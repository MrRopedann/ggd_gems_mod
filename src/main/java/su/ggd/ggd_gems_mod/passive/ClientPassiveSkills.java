package su.ggd.ggd_gems_mod.passive;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ClientPassiveSkills {
    private ClientPassiveSkills() {}

    private static final Gson GSON = new Gson();

    private static Map<String, Integer> levels = new HashMap<>();

    public static void applyLevelsJson(String json) {
        try {
            Type t = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String, Integer> m = GSON.fromJson(json, t);
            if (m == null) m = new HashMap<>();

            HashMap<String, Integer> norm = new HashMap<>();
            for (var e : m.entrySet()) {
                if (e.getKey() == null) continue;
                String id = e.getKey().toLowerCase(Locale.ROOT);
                int lv = e.getValue() == null ? 0 : e.getValue();
                if (lv > 0) norm.put(id, lv);
            }
            levels = norm;
        } catch (Exception ignored) {
            levels = new HashMap<>();
        }
    }

    public static int getLevel(String skillId) {
        if (skillId == null) return 0;
        return Math.max(0, levels.getOrDefault(skillId.toLowerCase(Locale.ROOT), 0));
    }

    public static Map<String, Integer> getAll() {
        return Collections.unmodifiableMap(levels);
    }
}
