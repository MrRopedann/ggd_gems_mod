package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.OnToolDamaged;
import su.ggd.ggd_gems_mod.passive.api.OnToolUseBefore;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

import java.util.HashMap;
import java.util.Map;

public final class LightHandEffect implements PassiveSkillEffect, OnToolDamaged, OnToolUseBefore {

    private static final double DEFAULT_CHANCE_PER_LEVEL = 0.0125; // 1.25%
    private static final int DEFAULT_REFUND = 1;

    @Override
    public String effectId() {
        return "light_hand";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        boolean changed = false;

        if (def.params == null) {
            def.params = new HashMap<>();
            changed = true;
        }

        if (!def.params.containsKey("chancePerLevel")) {
            def.params.put("chancePerLevel", DEFAULT_CHANCE_PER_LEVEL);
            changed = true;
        }

        if (!def.params.containsKey("refund")) {
            def.params.put("refund", DEFAULT_REFUND);
            changed = true;
        }

        return changed;
    }

    /**
     * Предохранитель от поломки на последней прочности (без mixin).
     * Вызывается ПЕРЕД действием, которое может потратить прочность.
     *
     * Если осталось 1 прочность и шанс сработал — откатываем damage на 1 заранее,
     * чтобы инструмент не исчез из стака.
     */
    @Override
    public void onToolUseBefore(ServerWorld world, ServerPlayerEntity player, ItemStack tool,
                                PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (world == null || player == null || tool == null || tool.isEmpty()) return;
        if (level <= 0) return;

        if (!tool.isDamageable()) return;

        int max = tool.getMaxDamage();
        int dmg = tool.getDamage();
        if (max <= 0) return;

        // осталось больше 1 прочности — предохранитель не нужен
        if (dmg < max - 1) return;

        Map<String, Object> params = (def == null) ? null : def.params;

        double chancePerLevel = JsonParamUtil.getDouble(params, "chancePerLevel", DEFAULT_CHANCE_PER_LEVEL);
        double chance = MathHelper.clamp(chancePerLevel * (double) level, 0.0, 1.0);
        if (chance <= 0.0) return;

        if (world.getRandom().nextDouble() >= chance) return;

        // +1 прочность до действия
        tool.setDamage(Math.max(0, dmg - 1));
    }

    /**
     * Пост-фактум компенсация: если прочность реально потратилась (diff>0),
     * с шансом откатываем damage на refund (обычно 1).
     */
    @Override
    public void onToolDamaged(ServerWorld world, ServerPlayerEntity player, ItemStack tool, int damageTaken,
                              PassiveSkillsConfig.PassiveSkillDef def, int level) {
        if (world == null || player == null || tool == null || tool.isEmpty()) return;
        if (damageTaken <= 0 || level <= 0) return;
        if (!tool.isDamageable()) return;

        Map<String, Object> params = (def == null) ? null : def.params;

        double chancePerLevel = JsonParamUtil.getDouble(params, "chancePerLevel", DEFAULT_CHANCE_PER_LEVEL);
        double chance = MathHelper.clamp(chancePerLevel * (double) level, 0.0, 1.0);

        if (world.getRandom().nextDouble() >= chance) return;

        int cur = tool.getDamage();
        int newDamage = Math.max(0, cur - damageTaken);
        if (newDamage != cur) tool.setDamage(newDamage);

        System.out.println("[light_hand] level=" + level + " damageTaken=" + damageTaken + " chance=" + chance);
    }


}
