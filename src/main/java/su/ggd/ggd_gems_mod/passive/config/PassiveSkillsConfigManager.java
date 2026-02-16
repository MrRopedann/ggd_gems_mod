package su.ggd.ggd_gems_mod.passive.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import net.fabricmc.loader.api.FabricLoader;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffects;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Works with your PassiveSkillsConfig shape:
 * - skills[].params is Map<String,Object>
 * - tabs[].skillIds
 * - indexes: byId, tabById via rebuildIndex()
 *
 * Also keeps compatibility with older configs where:
 * - skills[] had "effect" field (will be ignored by Gson now)
 * - or had legacy nested objects (oakRoots/luckyMiner/...) (ignored now)
 *
 * NOTE:
 * Since PassiveSkillDef no longer has "effect", this manager assumes effectId == skillId
 * when calling PassiveSkillEffects.get(skillId).
 */
public final class PassiveSkillsConfigManager {
    private PassiveSkillsConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("ggd_gems_mod");
    private static final Path FILE = DIR.resolve("passive_skills.json");

    private static PassiveSkillsConfig config = new PassiveSkillsConfig();

    public static PassiveSkillsConfig get() {
        return config;
    }

    public static void loadOrCreateDefault() {
        try {
            Files.createDirectories(DIR);

            if (!Files.exists(FILE)) {
                PassiveSkillsConfig def = buildDefault();
                normalizeAndReindex(def);
                write(def);
                config = def;
                return;
            }

            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            PassiveSkillsConfig loaded = GSON.fromJson(json, PassiveSkillsConfig.class);
            config = (loaded == null) ? new PassiveSkillsConfig() : loaded;

            boolean changed = normalizeAndReindex(config);
            if (changed) write(config);

        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to load/create passive skills config: " + FILE);
            e.printStackTrace();

            PassiveSkillsConfig def = buildDefault();
            normalizeAndReindex(def);
            config = def;

            try { write(def); } catch (IOException ignored) {}
        }
    }

    private static boolean normalizeAndReindex(PassiveSkillsConfig cfg) {
        boolean changed = false;

        if (cfg.skills == null) {
            cfg.skills = new ArrayList<>();
            changed = true;
        }
        if (cfg.tabs == null) {
            cfg.tabs = new ArrayList<>();
            changed = true;
        }

        // ---- normalize skills ----
        for (var def : cfg.skills) {
            if (def == null) continue;

            // id required
            if (def.id == null || def.id.isBlank()) continue;

            if (def.maxLevel <= 0) { def.maxLevel = 10; changed = true; }
            if (def.costBase < 0) { def.costBase = 0; changed = true; }
            if (def.costPerLevel < 0) { def.costPerLevel = 0; changed = true; }

            if (def.params == null) {
                def.params = new HashMap<>();
                changed = true;
            }

            // ---- ensure default skills exist ----
            changed |= ensureSkillExists(cfg,
                    "skin_mutation",
                    "Мутация кожи",
                    "Если моб наносит вам урон, он получает отражённый урон: 0.15 за уровень (на 10 уровне — 1.5).",
                    "minecraft:netherite_chestplate"
            );

            changed |= ensureSkillExistsFixed(cfg,
                    "beast_instinct",
                    "Звериная интуиция",
                    "Пассивные мобы (коровы, свиньи и т.д.) не убегают, если их атаковать.",
                    "minecraft:wheat",
                    1, 50, 0
            );

            changed |= ensureSkillExists(cfg,
                    "damage_absorption",
                    "Поглощение урона",
                    "Уменьшает входящий урон на 0.5% за уровень (на 10 уровне — 5%).",
                    "minecraft:shield"
            );

            changed |= ensureSkillExists(cfg,
                    "vampirism",
                    "Вампиризм",
                    "При нанесении урона: 0.5% шанс за уровень восстановить 1 сердечко (2 HP).",
                    "minecraft:ghast_tear"
            );



            // fill defaults for known skills if params empty
            changed |= fillDefaultsIfEmpty(def);

            // normalize via effect (effectId == skillId)
            PassiveSkillEffect eff = PassiveSkillEffects.get(def.id);
            if (eff != null) {
                try {
                    boolean effChanged = eff.normalize(def); // your normalize(def) likely reads/writes def.params/runtime
                    changed |= effChanged;
                } catch (Exception ignored) {}
            }
        }

        // ---- normalize tabs ----
        changed |= normalizeTabs(cfg);

        // ---- rebuild indexes ----
        cfg.rebuildIndex();

        return changed;
    }

    private static boolean ensureSkillExists(
            PassiveSkillsConfig cfg,
            String id,
            String name,
            String desc,
            String icon
    ) {
        if (cfg.skills == null) cfg.skills = new ArrayList<>();

        for (var s : cfg.skills) {
            if (s != null && id.equalsIgnoreCase(s.id)) {
                return false; // уже есть
            }
        }

        PassiveSkillsConfig.PassiveSkillDef s = skill(id, name, desc, icon);
        cfg.skills.add(s);
        return true;
    }


    private static boolean normalizeTabs(PassiveSkillsConfig cfg) {
        boolean changed = false;

        // if tabs empty -> create defaults
        if (cfg.tabs.isEmpty() && cfg.skills != null && !cfg.skills.isEmpty()) {
            cfg.tabs.add(tab("gathering", "Добыча", "minecraft:iron_pickaxe",
                    List.of("oak_roots", "lucky_miner", "diligent_student")));
            cfg.tabs.add(tab("combat", "Бой", "minecraft:iron_sword",
                    List.of("melee_damage", "ranged_damage", "bow_trajectory")));
            cfg.tabs.add(tab("survival", "Выживание", "minecraft:golden_apple",
                    List.of("bonus_hp")));
            cfg.tabs.add(tab("movement", "Передвижение", "minecraft:leather_boots",
                    List.of("move_speed")));
            changed = true;
        }

        // collect existing skill ids (lowered like your byId does)
        Set<String> existing = new HashSet<>();
        if (cfg.skills != null) {
            for (var s : cfg.skills) {
                if (s == null || s.id == null || s.id.isBlank()) continue;
                existing.add(s.id.toLowerCase(Locale.ROOT));
            }
        }

        // validate tabs
        Set<String> seenTabIds = new HashSet<>();
        for (int i = 0; i < cfg.tabs.size(); i++) {
            var t = cfg.tabs.get(i);
            if (t == null) continue;

            if (t.id == null || t.id.isBlank()) {
                t.id = "tab_" + i;
                changed = true;
            }
            if (!seenTabIds.add(t.id)) {
                t.id = t.id + "_" + i;
                changed = true;
            }
            if (t.name == null || t.name.isBlank()) {
                t.name = t.id;
                changed = true;
            }
            if (t.skillIds == null) {
                t.skillIds = new ArrayList<>();
                changed = true;
            }

            // remove missing skills
            if (!t.skillIds.isEmpty()) {
                List<String> cleaned = new ArrayList<>(t.skillIds.size());
                for (String sid : t.skillIds) {
                    if (sid == null || sid.isBlank()) { changed = true; continue; }
                    if (existing.contains(sid.toLowerCase(Locale.ROOT))) cleaned.add(sid);
                    else changed = true;
                }
                if (!cleaned.equals(t.skillIds)) {
                    t.skillIds = cleaned;
                    changed = true;
                }
            }
        }

        // if some skills are not listed in any tab -> append to first tab
        if (!cfg.skills.isEmpty()) {
            Set<String> listed = new HashSet<>();
            for (var t : cfg.tabs) {
                if (t == null || t.skillIds == null) continue;
                for (String sid : t.skillIds) if (sid != null) listed.add(sid.toLowerCase(Locale.ROOT));
            }
            List<String> missing = new ArrayList<>();
            for (var s : cfg.skills) {
                if (s == null || s.id == null) continue;
                String id = s.id.toLowerCase(Locale.ROOT);
                if (!listed.contains(id)) missing.add(s.id);
            }
            if (!missing.isEmpty()) {
                if (cfg.tabs.isEmpty()) {
                    cfg.tabs.add(tab("all", "Все", "minecraft:book", new ArrayList<>()));
                    changed = true;
                }
                cfg.tabs.get(0).skillIds.addAll(missing);
                changed = true;
            }
        }

        return changed;
    }

    private static PassiveSkillsConfig.TabDef tab(String id, String name, String iconItem, List<String> skillIds) {
        PassiveSkillsConfig.TabDef t = new PassiveSkillsConfig.TabDef();
        t.id = id;
        t.name = name;
        t.iconItem = iconItem;
        t.skillIds = new ArrayList<>(skillIds);
        return t;
    }

    private static boolean fillDefaultsIfEmpty(PassiveSkillsConfig.PassiveSkillDef def) {
        if (def.params == null) def.params = new HashMap<>();
        if (!def.params.isEmpty()) return false;

        String id = def.id.toLowerCase(Locale.ROOT);
        boolean changed = false;

        switch (id) {
            case "oak_roots" -> {
                def.params.put("speedPercentPerLevel", 0.05);
                def.params.put("speedCapLevel", 9);
                def.params.put("treeFellerChancePerLevel", 0.01);
                def.params.put("treeFellerLevel", 10);
                def.params.put("breakLeaves", true);
                def.params.put("radius", 8);
                def.params.put("maxBlocks", 256);
                changed = true;
            }
            case "lucky_miner" -> {
                def.params.put("speedPercentPerLevel", 0.05);
                def.params.put("doubleDropChancePerLevel", 0.01);
                def.params.put("speedCapLevel", 10);
                def.params.put("oreBlockTags", List.of(
                        "minecraft:coal_ores",
                        "minecraft:iron_ores",
                        "minecraft:gold_ores",
                        "minecraft:copper_ores",
                        "minecraft:diamond_ores",
                        "minecraft:emerald_ores",
                        "minecraft:lapis_ores",
                        "minecraft:redstone_ores"
                ));
                changed = true;
            }
            case "diligent_student" -> {
                def.params.put("xpBonusPercentPerLevel", 0.025);
                changed = true;
            }
            case "bow_trajectory" -> {
                def.params.put("steps", 60);
                def.params.put("drag", 0.99);
                def.params.put("gravity", 0.05);
                def.params.put("minPull", 0.10);

                def.params.put("outerWidth", 4.0);
                def.params.put("innerWidth", 2.0);
                def.params.put("markerSize", 0.18);
                def.params.put("markerWidth", 3.0);
                changed = true;
            }
            case "light_hand" -> {
                def.params.put("chancePerLevel", 0.0125);
                def.params.put("refund", 1);
                changed = true;
            }
            case "skin_mutation" -> {
                def.params.put("damagePerLevel", 0.15);
                changed = true;
            }
            case "literate_creator" -> {
                def.params.put("chancePerLevel", 0.015);
                changed = true;
            }
            case "damage_absorption" -> {
                def.params.put("absorbPercentPerLevel", 0.5);
                changed = true;
            }
            case "vampirism" -> {
                def.params.put("chancePercentPerLevel", 0.5);
                def.params.put("healAmount", 2.0); // 1 сердечко
                changed = true;
            }


            default -> {}
        }

        return changed;
    }

    private static PassiveSkillsConfig buildDefault() {
        PassiveSkillsConfig def = new PassiveSkillsConfig();

        def.skills.add(skill(
                "oak_roots",
                "Корни дуба",
                "Ускоряет рубку дерева топором на 5% за уровень до 9 уровня. На 10 уровне: ломаете 1 блок — падает всё дерево вместе с листвой.",
                "minecraft:oak_log"
        ));

        def.skills.add(skill(
                "lucky_miner",
                "Удачливый рудокоп",
                "Ускоряет добычу руды киркой на 5% за уровень. При добыче руды есть шанс на двойной дроп: 1% за уровень.",
                "minecraft:diamond_pickaxe"
        ));

        def.skills.add(skill(
                "diligent_student",
                "Прилежный ученик",
                "Увеличивает получаемый опыт на 2.5% за уровень.",
                "minecraft:experience_bottle"
        ));
        def.skills.add(skill(
                "light_hand",
                "Легкая рука",
                "Дает шанс 1.25% за уровень, что инструмент (кирка/топор/лопата/мотыга) при использовании не потратит прочность. ",
                "minecraft:iron_pickaxe"
        ));

        def.skills.add(skillFixed(
                "iron_stomach",
                "Железный желудок",
                "Гнилую плоть и сырую курятину можно есть без голода и отравления (но мало насыщает). Сырое мясо можно есть без штрафов.",
                "minecraft:rotten_flesh",
                1,   // maxLevel
                50,  // costBase (50 уровней персонажа)
                0    // costPerLevel
        ));
        def.skills.add(skill(
                "skin_mutation",
                "Мутация кожи",
                "Если моб наносит вам урон, он получает отражённый урон: 0.15 за уровень (на 10 уровне — 1.5).",
                "minecraft:netherite_chestplate"
        ));
        def.skills.add(skillFixed(
                "beast_instinct",
                "Звериная интуиция",
                "Пассивные мобы (коровы, свиньи и т.д.) не убегают, если их атаковать.",
                "minecraft:wheat",
                1,   // maxLevel
                50,  // costBase
                0    // costPerLevel
        ));
        def.skills.add(skill(
                "literate_creator",
                "Грамотный создатель",
                "При крафте инструментов, оружия и брони даёт 1.5% шанс за уровень, что случайный материал из рецепта не израсходуется (на 10 ур. — 15%).",
                "minecraft:crafting_table"
        ));
        def.skills.add(skill(
                "damage_absorption",
                "Поглощение урона",
                "Уменьшает входящий урон на 0.5% за уровень (на 10 уровне — 5%).",
                "minecraft:shield"
        ));

        def.skills.add(skill(
                "vampirism",
                "Вампиризм",
                "При нанесении урона: 0.5% шанс за уровень восстановить 1 сердечко (2 HP).",
                "minecraft:ghast_tear"
        ));






        // If these exist in your mod, keep them in default too
        def.skills.add(skill("melee_damage", "Сила удара", "Увеличивает урон ближнего боя.", "minecraft:iron_sword"));
        def.skills.add(skill("ranged_damage", "Меткий стрелок", "Увеличивает урон дальнего боя.", "minecraft:bow"));
        def.skills.add(skill("move_speed", "Лёгкая поступь", "Увеличивает скорость передвижения.", "minecraft:leather_boots"));
        def.skills.add(skill("bonus_hp", "Крепкое здоровье", "Увеличивает максимальное здоровье.", "minecraft:golden_apple"));
        def.skills.add(skill("bow_trajectory", "Траектория стрелы", "Показывает траекторию полёта стрелы при натяжении лука.", "minecraft:arrow"));
        def.skills.add(skill("light_hand", "Легкая рука", "Дает шанс 1.25% за уровень, что инструмент (кирка/топор/лопата/мотыга) при использовании не потратит прочность. На 10 уровне: 12.5%.", "minecraft:iron_pickaxe"));

        // tabs
        def.tabs.add(tab("gathering", "Добыча", "minecraft:iron_pickaxe",
                List.of("oak_roots", "lucky_miner", "light_hand")));
        def.tabs.add(tab("combat", "Бой", "minecraft:iron_sword",
                List.of("melee_damage", "ranged_damage", "bow_trajectory", "damage_absorption", "vampirism")));
        def.tabs.add(tab("survival", "Выживание", "minecraft:golden_apple",
                List.of("bonus_hp", "iron_stomach", "beast_instinct", "literate_creator")));
        def.tabs.add(tab("movement", "Передвижение", "minecraft:leather_boots",
                List.of("move_speed", "diligent_student")));




        def.rebuildIndex();
        return def;
    }

    private static PassiveSkillsConfig.PassiveSkillDef skill(String id, String name, String desc, String icon) {
        PassiveSkillsConfig.PassiveSkillDef s = new PassiveSkillsConfig.PassiveSkillDef();
        s.id = id;
        s.name = name;
        s.description = desc;
        s.maxLevel = 10;
        s.iconItem = icon;
        s.costBase = 5;
        s.costPerLevel = 5;
        s.params = new HashMap<>();
        fillDefaultsIfEmpty(s);
        return s;
    }

    private static PassiveSkillsConfig.PassiveSkillDef skillFixed(
            String id, String name, String desc, String icon,
            int maxLevel, int costBase, int costPerLevel
    ) {
        PassiveSkillsConfig.PassiveSkillDef s = new PassiveSkillsConfig.PassiveSkillDef();
        s.id = id;
        s.name = name;
        s.description = desc;
        s.maxLevel = maxLevel;
        s.iconItem = icon;
        s.costBase = costBase;
        s.costPerLevel = costPerLevel;
        s.params = new HashMap<>();
        fillDefaultsIfEmpty(s);
        return s;
    }


    private static void write(PassiveSkillsConfig cfg) throws IOException {
        String json = GSON.toJson(cfg);
        Files.writeString(
                FILE,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    // ===== NETWORK SYNC SUPPORT =====

    public static String toJsonString() {
        return GSON.toJson(config);
    }

    public static void applyFromNetworkJson(String json) {
        try {
            PassiveSkillsConfig loaded = GSON.fromJson(json, PassiveSkillsConfig.class);
            PassiveSkillsConfig next = (loaded == null) ? new PassiveSkillsConfig() : loaded;
            normalizeAndReindex(next);
            config = next;
        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to apply network passive skills config");
            e.printStackTrace();
        }
    }

    public static String sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private static boolean ensureSkillExistsFixed(PassiveSkillsConfig cfg, String id, String name, String desc, String icon,
                                                  int maxLevel, int costBase, int costPerLevel) {
        if (cfg.skills == null) cfg.skills = new ArrayList<>();
        for (var s : cfg.skills) {
            if (s != null && id.equalsIgnoreCase(s.id)) return false;
        }
        cfg.skills.add(skillFixed(id, name, desc, icon, maxLevel, costBase, costPerLevel));
        return true;
    }

}
