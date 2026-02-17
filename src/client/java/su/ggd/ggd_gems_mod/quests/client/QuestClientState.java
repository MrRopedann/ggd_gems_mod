package su.ggd.ggd_gems_mod.quests.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import su.ggd.ggd_gems_mod.quests.state.QuestsState;

public final class QuestClientState {
    private QuestClientState() {}

    private static final Gson GSON = new GsonBuilder().create();

    private static QuestsState.PlayerQuestData playerData = new QuestsState.PlayerQuestData();

    public static QuestsState.PlayerQuestData getPlayerData() {
        return playerData;
    }

    public static void applyPlayerJson(String json) {
        try {
            QuestsState.PlayerQuestData d = GSON.fromJson(json, QuestsState.PlayerQuestData.class);
            if (d != null) playerData = d;
        } catch (Exception ignored) {
        }
    }
}
