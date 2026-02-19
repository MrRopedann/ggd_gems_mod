package su.ggd.ggd_gems_mod.currency;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import su.ggd.ggd_gems_mod.currency.net.CurrencyNet;
import su.ggd.ggd_gems_mod.currency.state.CurrencyState;

public final class CurrencyService {
    private CurrencyService() {}

    public static long getBalance(ServerPlayerEntity player) {
        if (player == null) return 0L;
        if (!(player.getEntityWorld() instanceof ServerWorld sw)) return 0L;
        return CurrencyState.get(sw).getBalance(player.getUuid());
    }

    public static void setBalance(ServerPlayerEntity player, long value) {
        if (player == null) return;
        if (!(player.getEntityWorld() instanceof ServerWorld sw)) return;

        long v = Math.max(0L, value);
        CurrencyState.get(sw).setBalance(player.getUuid(), v);
        sync(player, v);
    }

    public static void add(ServerPlayerEntity player, long delta) {
        if (player == null || delta <= 0L) return;
        setBalance(player, getBalance(player) + delta);
    }

    public static boolean trySpend(ServerPlayerEntity player, long amount) {
        if (player == null) return false;
        if (amount <= 0L) return true;

        long cur = getBalance(player);
        if (cur < amount) return false;

        setBalance(player, cur - amount);
        return true;
    }

    public static void sync(ServerPlayerEntity player, long balance) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new CurrencyNet.SyncBalancePayload(Math.max(0L, balance)));
    }

    public static void syncCurrent(ServerPlayerEntity player) {
        if (player == null) return;
        sync(player, getBalance(player));
    }
}
