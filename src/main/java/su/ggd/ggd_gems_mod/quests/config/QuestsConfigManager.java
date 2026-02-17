package su.ggd.ggd_gems_mod.quests.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Loads quests from config/ggd_gems_mod/quests/<questFolder>/
 *  - quest.json (required)
 *  - stages.json (required)
 *  - dialogs.json (optional)
 *
 * Server is the source of truth; client receives json via sync and calls applyFromNetworkJson.
 */
public final class QuestsConfigManager {
    private QuestsConfigManager() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path ROOT_DIR = FabricLoader.getInstance().getConfigDir()
            .resolve("ggd_gems_mod")
            .resolve("quests");

    private static QuestsDatabase db = new QuestsDatabase();

    public static QuestsDatabase get() {
        return db;
    }

    public static void loadOrCreateDefault() {
        try {
            Files.createDirectories(ROOT_DIR);

            boolean hasAny = hasAnyQuestFolders(ROOT_DIR);
            if (!hasAny) {
                createExampleQuest(ROOT_DIR);
            }

            QuestsDatabase loaded = loadAll(ROOT_DIR);
            boolean changed = normalize(loaded);

            db = loaded;

            // We do NOT rewrite user files on every normalize.
            // Only create example if empty. Validation errors are logged and skipped.
            if (changed) {
                // no-op for now (keeps user configs intact)
            }

        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to load quests from: " + ROOT_DIR);
            e.printStackTrace();
            db = new QuestsDatabase();
        }
    }

    public static String toJsonString() {
        return GSON.toJson(db);
    }

    public static void applyFromNetworkJson(String json) {
        try {
            QuestsDatabase next = GSON.fromJson(json, QuestsDatabase.class);
            if (next == null) next = new QuestsDatabase();
            normalize(next);
            db = next;
        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to apply network quests defs");
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
            return "";
        }
    }

    // ---- load helpers ----

    private static boolean hasAnyQuestFolders(Path root) {
        try (var stream = Files.list(root)) {
            return stream.anyMatch(p -> Files.isDirectory(p) && Files.exists(p.resolve("quest.json")));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static QuestsDatabase loadAll(Path root) {
        QuestsDatabase out = new QuestsDatabase();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                try {
                    Path questFile = dir.resolve("quest.json");
                    Path stagesFile = dir.resolve("stages.json");
                    if (!Files.exists(questFile) || !Files.exists(stagesFile)) return;

                    String questJson = Files.readString(questFile, StandardCharsets.UTF_8);
                    QuestDef quest = GSON.fromJson(questJson, QuestDef.class);
                    if (quest == null) return;

                    if (quest.id == null || quest.id.isBlank()) {
                        quest.id = dir.getFileName().toString();
                    }

                    List<StageDef> stages = readStages(stagesFile);
                    if (stages == null) stages = new ArrayList<>();

                    List<DialogDef> dialogs = readDialogs(dir.resolve("dialogs.json"));

                    QuestsDatabase.QuestFullDef full = new QuestsDatabase.QuestFullDef();
                    full.quest = quest;
                    full.stages = stages;
                    full.dialogs = dialogs;

                    out.quests.add(full);

                } catch (Exception e) {
                    System.err.println("[ggd_gems_mod] Failed to load quest folder: " + dir);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to scan quests dir: " + root);
            e.printStackTrace();
        }
        out.rebuildIndex();
        return out;
    }

    private static List<StageDef> readStages(Path stagesFile) {
        try {
            if (!Files.exists(stagesFile)) return new ArrayList<>();
            String json = Files.readString(stagesFile, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) return new ArrayList<>();

            if (json.startsWith("[")) {
                Type t = new TypeToken<List<StageDef>>(){}.getType();
                List<StageDef> list = GSON.fromJson(json, t);
                return list == null ? new ArrayList<>() : list;
            } else {
                // support wrapper: { "stages": [...] }
                JsonElement el = JsonParser.parseString(json);
                if (el != null && el.isJsonObject() && el.getAsJsonObject().has("stages")) {
                    Type t = new TypeToken<List<StageDef>>(){}.getType();
                    List<StageDef> list = GSON.fromJson(el.getAsJsonObject().get("stages"), t);
                    return list == null ? new ArrayList<>() : list;
                }
            }
        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to read stages file: " + stagesFile);
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private static List<DialogDef> readDialogs(Path dialogsFile) {
        try {
            if (!Files.exists(dialogsFile)) return new ArrayList<>();
            String json = Files.readString(dialogsFile, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) return new ArrayList<>();

            if (json.startsWith("[")) {
                Type t = new TypeToken<List<DialogDef>>(){}.getType();
                List<DialogDef> list = GSON.fromJson(json, t);
                return list == null ? new ArrayList<>() : list;
            } else {
                // support wrapper: { "dialogs": [...] }
                JsonElement el = JsonParser.parseString(json);
                if (el != null && el.isJsonObject() && el.getAsJsonObject().has("dialogs")) {
                    Type t = new TypeToken<List<DialogDef>>(){}.getType();
                    List<DialogDef> list = GSON.fromJson(el.getAsJsonObject().get("dialogs"), t);
                    return list == null ? new ArrayList<>() : list;
                }
            }
        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to read dialogs file: " + dialogsFile);
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ---- normalization/validation ----

    private static boolean normalize(QuestsDatabase d) {
        boolean changed = false;
        if (d == null) return false;

        if (d.quests == null) {
            d.quests = new ArrayList<>();
            changed = true;
        }

        HashSet<String> ids = new HashSet<>();
        ArrayList<QuestsDatabase.QuestFullDef> ok = new ArrayList<>();

        for (QuestsDatabase.QuestFullDef q : d.quests) {
            if (q == null || q.quest == null) continue;

            String id = safeId(q.quest.id);
            if (id == null) continue;

            q.quest.id = id;

            if (ids.contains(id)) {
                System.err.println("[ggd_gems_mod] Duplicate quest id: " + id + " (skipped)");
                continue;
            }
            ids.add(id);

            if (q.stages == null) q.stages = new ArrayList<>();
            if (q.dialogs == null) q.dialogs = new ArrayList<>();

            // normalize stages
            HashSet<String> stageIds = new HashSet<>();
            ArrayList<StageDef> stagesOk = new ArrayList<>();
            for (StageDef s : q.stages) {
                if (s == null) continue;
                s.id = safeId(s.id);
                if (s.id == null) continue;

                if (stageIds.contains(s.id)) {
                    System.err.println("[ggd_gems_mod] Duplicate stage id in quest " + id + ": " + s.id + " (skipped)");
                    continue;
                }
                stageIds.add(s.id);

                if (s.objectives == null) s.objectives = new ArrayList<>();
                HashSet<String> objIds = new HashSet<>();
                ArrayList<ObjectiveDef> objOk = new ArrayList<>();
                for (ObjectiveDef o : s.objectives) {
                    if (o == null) continue;
                    o.id = safeId(o.id);
                    if (o.id == null) continue;
                    if (objIds.contains(o.id)) {
                        System.err.println("[ggd_gems_mod] Duplicate objective id in quest " + id + "/" + s.id + ": " + o.id + " (skipped)");
                        continue;
                    }
                    objIds.add(o.id);

                    if (o.type != null) o.type = o.type.trim().toUpperCase(Locale.ROOT);
                    if (o.required <= 0) o.required = 1;

                    if (o.target == null) o.target = new HashMap<>();
                    if (o.constraints == null) o.constraints = new HashMap<>();

                    objOk.add(o);
                }
                s.objectives = objOk;

                stagesOk.add(s);
            }
            q.stages = stagesOk;

            // start stage default
            if ((q.quest.startStageId == null || q.quest.startStageId.isBlank()) && !q.stages.isEmpty()) {
                q.quest.startStageId = q.stages.get(0).id;
                changed = true;
            } else if (q.quest.startStageId != null) {
                q.quest.startStageId = safeId(q.quest.startStageId);
            }

            // validate nextStageId existence (V1 linear)
            for (StageDef s : q.stages) {
                if (s == null) continue;
                if (s.nextStageId != null && !s.nextStageId.isBlank()) {
                    s.nextStageId = safeId(s.nextStageId);
                    if (s.nextStageId != null && !stageIds.contains(s.nextStageId)) {
                        System.err.println("[ggd_gems_mod] Quest " + id + " stage " + s.id + " has missing nextStageId: " + s.nextStageId + " (cleared)");
                        s.nextStageId = null;
                        changed = true;
                    }
                }
            }

            ok.add(q);
        }

        d.quests = ok;
        d.rebuildIndex();
        return changed;
    }

    private static String safeId(String s) {
        if (s == null) return null;
        String id = s.trim();
        if (id.isEmpty()) return null;
        return id;
    }

    // ---- example quest ----

    private static void createExampleQuest(Path root) {
        try {
            Path qDir = root.resolve("example_kill_zombie");
            Files.createDirectories(qDir);

            Path questFile = qDir.resolve("quest.json");
            Path stagesFile = qDir.resolve("stages.json");

            if (!Files.exists(questFile)) {
                String questJson = """
                        {
                          "formatVersion": 1,
                          "id": "example_kill_zombie",
                          "name": "Первое испытание",
                          "description": "Убей 3 зомби.",
                          "category": "story",
                          "iconItem": "minecraft:iron_sword",
                          "repeatable": false,
                          "startStageId": "stage1"
                        }
                        """;
                Files.writeString(questFile, questJson.stripTrailing(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            if (!Files.exists(stagesFile)) {
                String stagesJson = """
                        [
                          {
                            "id": "stage1",
                            "name": "Охота",
                            "description": "Найди и убей зомби.",
                            "objectives": [
                              {
                                "id": "kill_zombie",
                                "type": "KILL",
                                "required": 3,
                                "target": {
                                  "entityType": "minecraft:zombie"
                                }
                              }
                            ],
                            "nextStageId": null
                          }
                        ]
                        """;
                Files.writeString(stagesFile, stagesJson.stripTrailing(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

        } catch (Exception e) {
            System.err.println("[ggd_gems_mod] Failed to create example quest in: " + root);
            e.printStackTrace();
        }
    }
}
