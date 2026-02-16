package su.ggd.ggd_gems_mod.passive.effects;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import su.ggd.ggd_gems_mod.passive.api.JsonParamUtil;
import su.ggd.ggd_gems_mod.passive.api.OnEntityDamagedByPlayer;
import su.ggd.ggd_gems_mod.passive.api.PassiveSkillEffect;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public final class VampirismEffect implements PassiveSkillEffect, OnEntityDamagedByPlayer {

    @Override
    public String effectId() {
        return "vampirism";
    }

    @Override
    public boolean normalize(PassiveSkillsConfig.PassiveSkillDef def) {
        return false;
    }

    @Override
    public void onEntityDamagedByPlayer(ServerWorld world, ServerPlayerEntity player, LivingEntity victim,
                                        DamageSource source, float amount,
                                        PassiveSkillsConfig.PassiveSkillDef def, int level) {

        if (level <= 0 || amount <= 0f) return;

        double chancePerLevelPercent =
                JsonParamUtil.getDouble(def.params, "chancePercentPerLevel", 0.5);

        double chance = (chancePerLevelPercent * level) / 100.0;
        if (chance <= 0.0) return;

        if (player.getRandom().nextDouble() < chance) {

            float heal = (float) JsonParamUtil.getDouble(def.params, "healAmount", 2.0);
            if (heal > 0f && player.isAlive()) {

                player.heal(heal);

                player.sendMessage(
                        Text.literal("§cВампиризм сработал! §f+" + (heal / 2f) + "❤"),
                        true
                );
            }
        }
    }
}
