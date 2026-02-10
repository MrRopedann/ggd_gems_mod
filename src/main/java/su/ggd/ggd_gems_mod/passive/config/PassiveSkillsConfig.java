package su.ggd.ggd_gems_mod.passive.config;

import com.google.gson.JsonObject;

import java.util.*;

public class PassiveSkillsConfig {
    public List<PassiveSkillDef> skills = new ArrayList<>();
    public List<TabDef> tabs = new ArrayList<>();

    // индекс по id (НЕ сериализуем)
    public transient Map<String, PassiveSkillDef> byId = new HashMap<>();
    public transient Map<String, TabDef> tabById = new HashMap<>();

    public void rebuildIndex() {
        byId.clear();
        if (skills == null) skills = new ArrayList<>();
        for (PassiveSkillDef s : skills) {
            if (s == null) continue;
            if (s.id == null || s.id.isBlank()) continue;
            byId.put(s.id.toLowerCase(Locale.ROOT), s);
            if (s.runtime == null) s.runtime = new HashMap<>();
            if (s.params == null) s.params = new HashMap<>();
        }
        tabById.clear();
        for (var t : tabs) {
            if (t == null || t.id == null) continue;
            tabById.put(t.id, t);
        }

    }

    public static class TabDef {
        public String id;
        public String name;
        public String iconItem;
        public List<String> skillIds = new ArrayList<>();
    }

    public static class PassiveSkillDef {
        public String id;
        public String name;
        public String description;
        public int maxLevel = 1;
        public String iconItem;
        public int costBase = 0;
        public int costPerLevel = 0;
        public Map<String, Object> params = new HashMap<>();

        // НЕ сериализуем: кеши, скомпилированные теги и т.п.
        public transient Map<String, Object> runtime = new HashMap<>();
    }
}
