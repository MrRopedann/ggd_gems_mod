package su.ggd.ggd_gems_mod.passive;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsNet;
import su.ggd.ggd_gems_mod.passive.net.PassiveSkillsSyncServer;
import su.ggd.ggd_gems_mod.passive.state.PassiveSkillsState;

import java.util.Locale;

public final class PassiveSkillService {
    private PassiveSkillService() {}

    public static int costForNextLevel(PassiveSkillsConfig.PassiveSkillDef def, int nextLevel) {
        if (def == null) return Integer.MAX_VALUE;
        int lv = Math.max(1, nextLevel);
        long cost = (long) def.costBase + (long) (lv - 1) * (long) def.costPerLevel;
        if (cost < 0) cost = Integer.MAX_VALUE;
        if (cost > Integer.MAX_VALUE) cost = Integer.MAX_VALUE;
        return (int) cost;
    }

    public static void tryUpgrade(ServerPlayerEntity player, String skillIdRaw) {
        if (player == null) return;
        if (skillIdRaw == null || skillIdRaw.isBlank()) return;

        String skillId = skillIdRaw.toLowerCase(Locale.ROOT);

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        PassiveSkillsConfig.PassiveSkillDef def = (cfg == null || cfg.byId == null) ? null : cfg.byId.get(skillId);
        if (def == null) {
            PassiveSkillsNet.sendUpgradeResult(player, false, "Неизвестный навык: " + skillId);
            return;
        }

        // ✅ Всегда overworld
        ServerWorld sw = player.getEntityWorld().getServer().getOverworld();

        PassiveSkillsState state = PassiveSkillsState.get(sw);

        int current = state.getLevel(player.getUuid(), skillId);
        int next = current + 1;

        if (next > def.maxLevel) {
            PassiveSkillsNet.sendUpgradeResult(player, false, "Навык уже максимального уровня.");
            return;
        }

        int cost = costForNextLevel(def, next);

        if (player.experienceLevel < cost) {
            PassiveSkillsNet.sendUpgradeResult(player, false, "Нужно уровней: " + cost);
            return;
        }

        player.addExperienceLevels(-cost);
        state.setLevel(player.getUuid(), skillId, next); // внутри markDirty()

        // синк уровней обратно игроку (обновит UI и цену на кнопке)
        PassiveSkillsSyncServer.sendPlayerSkillsTo(player);

        // ✅ сообщение в окно
        PassiveSkillsNet.sendUpgradeResult(player, true, def.name + " улучшен до " + next + " ур. (-" + cost + ")");
    }

}
