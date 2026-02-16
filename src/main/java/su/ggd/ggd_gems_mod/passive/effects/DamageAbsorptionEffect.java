package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.ModifyIncomingDamage;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public final class DamageAbsorptionEffect implements PassiveSkillEffect, ModifyIncomingDamage {

    @Override
    public String effectId() {
        return "damage_absorption";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        return false;
    }

    @Override
    public float modifyIncomingDamage(ServerWorld world, ServerPlayerEntity player, DamageSource source,
                                      float amount, PassiveSkillsConfig.PassiveSkillDef def, int level) {

        if (level <= 0 || amount <= 0f) return amount;

        double perLevelPercent =
                JsonParamUtil.getDouble(def.params, "absorbPercentPerLevel", 0.5);

        double reduce = (perLevelPercent * level) / 100.0;
        if (reduce <= 0.0) return amount;
        if (reduce > 0.90) reduce = 0.90;

        float out = (float) (amount * (1.0 - reduce));

        if (out < amount) {
            float reduced = amount - out;

            player.sendMessage(
                    Text.literal("§bПоглощение урона сработало! §f-" + String.format("%.2f", reduced)),
                    true
            );
        }

        return Math.max(0f, out);
    }
}
