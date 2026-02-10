package su.ggd.ggd_gems_mod.passive.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;

public interface OnXpOrbPickup {
    int modifyXp(
            ServerWorld world,
            ServerPlayerEntity player,
            int currentXp,
            PassiveSkillsConfig.PassiveSkillDef def,
            int level
    );
}
