package su.ggd.ggd_gems_mod.quests.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import su.ggd.ggd_gems_mod.quests.client.QuestClientState;
import su.ggd.ggd_gems_mod.quests.config.ObjectiveDef;
import su.ggd.ggd_gems_mod.quests.config.QuestsConfigManager;
import su.ggd.ggd_gems_mod.quests.config.QuestsDatabase;
import su.ggd.ggd_gems_mod.quests.config.StageDef;
import su.ggd.ggd_gems_mod.quests.state.QuestsState;

import java.util.Map;

public final class QuestTrackerHud implements HudRenderCallback {

    public static void init() {
        HudRenderCallback.EVENT.register(new QuestTrackerHud());
    }

    @Override
    public void onHudRender(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        QuestsState.PlayerQuestData pd = QuestClientState.getPlayerData();
        if (pd == null) return;

        String trackedId = pd.trackedQuestId;
        if (trackedId == null || trackedId.isBlank()) return;

        QuestsState.ActiveQuestProgress pr = pd.activeQuests.get(trackedId);
        if (pr == null) return;

        QuestsDatabase db = QuestsConfigManager.get();
        if (db == null) return;
        if (db.byId == null || db.byId.isEmpty()) db.rebuildIndex();

        QuestsDatabase.QuestFullDef qdef = db.get(trackedId);
        if (qdef == null || qdef.quest == null) return;

        StageDef stage = findStage(qdef, pr.currentStageId);

        TextRenderer tr = client.textRenderer;

        int x = 8;
        int y = 8;

        // Header: Quest name
        String questName = (qdef.quest.name != null && !qdef.quest.name.isBlank()) ? qdef.quest.name : trackedId;
        ctx.drawText(tr, Text.literal(questName), x, y, 0xFFFFFFFF, true);
        y += 10;

        // Stage line
        if (stage != null) {
            String stageName = (stage.name != null && !stage.name.isBlank()) ? stage.name : stage.id;
            ctx.drawText(tr, Text.literal(stageName), x, y, 0xFFE0E0E0, true);
            y += 10;
        }

        // Objectives (2-4 lines)
        int lines = 0;
        if (stage != null && stage.objectives != null) {
            for (ObjectiveDef obj : stage.objectives) {
                if (obj == null || obj.hidden) continue;
                if (lines >= 4) break;

                int cur = pr.objectives.getOrDefault(obj.id, 0);
                int req = Math.max(1, obj.required);

                String label = formatObjectiveLabel(obj);
                String line = label + " " + cur + "/" + req;

                ctx.drawText(tr, Text.literal(line), x, y, 0xFFD0D0D0, true);
                y += 10;
                lines++;
            }
        }

        // progress bar (sum progress / sum required)
        if (stage != null && stage.objectives != null && !stage.objectives.isEmpty()) {
            int reqSum = 0;
            int curSum = 0;

            for (ObjectiveDef obj : stage.objectives) {
                if (obj == null || obj.hidden) continue;

                int req = Math.max(1, obj.required);
                int cur = pr.objectives.getOrDefault(obj.id, 0);

                reqSum += req;
                curSum += Math.min(cur, req);
            }

            int barW = 120;
            int barH = 6;

            int barX = x;
            int barY = y + 2;

            ctx.fill(barX, barY, barX + barW, barY + barH, 0x90000000);

            int filled = (reqSum <= 0) ? 0 : (int) Math.floor((curSum / (double) reqSum) * barW);
            if (filled > 0) {
                ctx.fill(barX, barY, barX + filled, barY + barH, 0x90FFFFFF);
            }
        }
    }

    private static StageDef findStage(QuestsDatabase.QuestFullDef def, String stageId) {
        if (def == null || def.stages == null || stageId == null) return null;
        for (StageDef s : def.stages) {
            if (s != null && stageId.equals(s.id)) return s;
        }
        return null;
    }

    private static String formatObjectiveLabel(ObjectiveDef obj) {
        if (obj == null) return "Objective";

        if (obj.uiText != null && !obj.uiText.isBlank()) {
            return obj.uiText;
        }

        String type = (obj.type == null) ? "OBJECTIVE" : obj.type.trim().toUpperCase();

        String targetHint = switch (type) {
            case "KILL" -> getString(obj.target, "entityType", "entity");
            case "BREAK_BLOCK" -> getString(obj.target, "blockId", "block");
            case "COLLECT_ITEM" -> getString(obj.target, "itemId", "item");
            case "TALK" -> getString(obj.target, "npcId", "npc");
            default -> null;
        };

        if (targetHint != null) return type + " (" + targetHint + ")";
        return type;
    }

    private static String getString(Map<String, Object> map, String k1, String k2) {
        if (map == null) return null;
        Object v = map.get(k1);
        if (v == null) v = map.get(k2);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }
}
