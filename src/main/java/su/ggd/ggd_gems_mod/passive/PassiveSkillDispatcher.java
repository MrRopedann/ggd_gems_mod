package su.ggd.ggd_gems_mod.passive;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import su.ggd.ggd_gems_mod.passive.api.*;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfig;
import su.ggd.ggd_gems_mod.passive.config.PassiveSkillsConfigManager;
import su.ggd.ggd_gems_mod.passive.state.PassiveSkillsState;
import su.ggd.ggd_gems_mod.passive.api.OnToolDamaged;
import su.ggd.ggd_gems_mod.passive.api.OnToolUseBefore;



import java.util.Locale;

// Если ClientPassiveSkills у тебя в этом пакете — импорт не нужен.
// Если он в другом пакете — добавь импорт сюда:
// import su.ggd.ggd_gems_mod.passive.ClientPassiveSkills;

public final class PassiveSkillDispatcher {
    private PassiveSkillDispatcher() {}

    public static void onToolUseBefore(ServerWorld world, ServerPlayerEntity player, ItemStack tool) {
        if (world == null || player == null || tool == null || tool.isEmpty()) return;

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null || cfg.skills == null) return;

        PassiveSkillsState st = PassiveSkillsState.get(world);

        for (PassiveSkillsConfig.PassiveSkillDef def : cfg.skills) {
            if (def == null) continue;

            String id = safeLower(def.id);
            if (id == null) continue;

            int level = st.getLevel(player.getUuid(), id);
            if (level <= 0) continue;

            PassiveSkillEffect eff = PassiveSkillEffects.get(def.id);
            if (eff instanceof OnToolUseBefore hook) {
                try {
                    hook.onToolUseBefore(world, player, tool, def, level);
                } catch (Exception ignored) {}
            }
        }
    }

    public static void onToolDamaged(ServerWorld world, ServerPlayerEntity player, ItemStack tool, int damageTaken) {
        if (world == null || player == null || tool == null || tool.isEmpty()) return;
        if (damageTaken <= 0) return;

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null || cfg.skills == null) return;

        PassiveSkillsState st = PassiveSkillsState.get(world);

        for (PassiveSkillsConfig.PassiveSkillDef def : cfg.skills) {
            if (def == null) continue;

            String id = safeLower(def.id);
            if (id == null) continue;

            int level = st.getLevel(player.getUuid(), id);
            if (level <= 0) continue;

            PassiveSkillEffect eff = PassiveSkillEffects.get(resolveEffectId(def));
            if (eff instanceof OnToolDamaged hook) {
                try {
                    hook.onToolDamaged(world, player, tool, damageTaken, def, level);
                } catch (Exception ignored) {}
            }
        }
    }

    private static String resolveEffectId(PassiveSkillsConfig.PassiveSkillDef def) {
        if (def == null) return null;
        if (def.id == null || def.id.isBlank()) return null;
        return def.id;
    }


    public static void onBlockBreakAfter(ServerWorld world, ServerPlayerEntity player, BlockPos pos, BlockState state, BlockEntity be) {
        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null || cfg.skills == null) return;

        PassiveSkillsState st = PassiveSkillsState.get(world);

        for (PassiveSkillsConfig.PassiveSkillDef def : cfg.skills) {
            if (def == null) continue;

            String id = safeLower(def.id);
            if (id == null) continue;

            int level = st.getLevel(player.getUuid(), id);
            if (level <= 0) continue;

            PassiveSkillEffect eff = PassiveSkillEffects.get(def.id);
            if (eff instanceof OnBlockBreakAfter hook) {
                try {
                    hook.onBlockBreakAfter(world, player, pos, state, be, def, level);
                } catch (Exception ignored) {}
            }
        }
    }

    public static float applyBreakSpeed(PlayerEntity player, BlockState state, ItemStack tool, float base) {
        if (player == null || state == null || tool == null || tool.isEmpty()) return base;

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null || cfg.skills == null) return base;

        // client
        if (player.getEntityWorld().isClient()) {
            float speed = base;

            for (PassiveSkillsConfig.PassiveSkillDef def : cfg.skills) {
                if (def == null) continue;
                String id = safeLower(def.id);
                if (id == null) continue;

                int level = ClientPassiveSkills.getLevel(id);
                if (level <= 0) continue;

                PassiveSkillEffect eff = PassiveSkillEffects.get(def.id);
                if (eff instanceof OnBreakSpeed hook) {
                    try { speed = hook.modifyBreakSpeed(player, state, tool, speed, def, level); }
                    catch (Exception ignored) {}
                }
            }
            return speed;
        }

        // server
        if (!(player instanceof ServerPlayerEntity sp)) return base;

        ServerWorld world = sp.getEntityWorld();
        PassiveSkillsState st = PassiveSkillsState.get(world);

        float speed = base;
        for (PassiveSkillsConfig.PassiveSkillDef def : cfg.skills) {
            if (def == null) continue;
            String id = safeLower(def.id);
            if (id == null) continue;

            int level = st.getLevel(sp.getUuid(), id);
            if (level <= 0) continue;

            PassiveSkillEffect eff = PassiveSkillEffects.get(def.id);
            if (eff instanceof OnBreakSpeed hook) {
                try { speed = hook.modifyBreakSpeed(player, state, tool, speed, def, level); }
                catch (Exception ignored) {}
            }
        }
        return speed;
    }


    public static int applyXpOrb(ServerWorld world, ServerPlayerEntity player, int baseXp) {
        if (baseXp <= 0) return baseXp;

        PassiveSkillsConfig cfg = PassiveSkillsConfigManager.get();
        if (cfg == null || cfg.skills == null) return baseXp;

        PassiveSkillsState st = PassiveSkillsState.get(world);

        int xp = baseXp;

        for (PassiveSkillsConfig.PassiveSkillDef def : cfg.skills) {
            if (def == null) continue;
            String id = safeLower(def.id);
            if (id == null) continue;

            int level = st.getLevel(player.getUuid(), id);
            if (level <= 0) continue;

            PassiveSkillEffect eff = PassiveSkillEffects.get(def.id);
            if (eff instanceof OnXpOrbPickup hook) {
                try {
                    xp = hook.modifyXp(world, player, xp, def, level);
                } catch (Exception ignored) {}
            }
        }

        return xp;
    }

    private static String safeLower(String s) {
        if (s == null || s.isBlank()) return null;
        return s.toLowerCase(Locale.ROOT);
    }
}
