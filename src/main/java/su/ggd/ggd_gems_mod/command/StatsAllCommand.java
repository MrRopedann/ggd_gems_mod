package su.ggd.ggd_gems_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class StatsAllCommand {
    private StatsAllCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("statsall")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayerOrThrow();

                            src.sendFeedback(() -> Text.literal("§6=== Все статы персонажа ==="), false);

                            for (RegistryEntry<EntityAttribute> entry : Registries.ATTRIBUTE.streamEntries().toList()) {
                                printAttribute(src, player, entry);
                            }


                            return 1;
                        })
        );
    }

    private static void printAttribute(
            ServerCommandSource src,
            ServerPlayerEntity player,
            RegistryEntry<EntityAttribute> entry
    ) {
        EntityAttributeInstance inst = player.getAttributeInstance(entry);
        if (inst == null) return; // атрибут есть в реестре, но не применяется к игроку

        Identifier id = Registries.ATTRIBUTE.getId(entry.value());
        if (id == null) return;

        double base = inst.getBaseValue();
        double total = inst.getValue();
        int modifiers = inst.getModifiers().size();

        src.sendFeedback(() -> Text.literal(String.format(Locale.US,
                "§e%s§7: base=§a%s§7, total=§b%s§7, mods=%d",
                id.toString(),
                fmt(base),
                fmt(total),
                modifiers
        )), false);
    }

    private static String fmt(double v) {
        String s = String.format(Locale.US, "%.5f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
