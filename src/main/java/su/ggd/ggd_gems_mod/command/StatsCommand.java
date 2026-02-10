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

public final class StatsCommand {
    private StatsCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("stats")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    ServerPlayerEntity p = src.getPlayerOrThrow();

                    src.sendFeedback(() -> Text.literal("§6=== Статы персонажа ==="), false);

                    sendAttr(src, p, "АТАКА", id("attack_damage"));
                    sendAttr(src, p, "ЗАЩИТА", id("armor"));
                    sendAttr(src, p, "ЗДОРОВЬЕ", id("max_health"));
                    sendAttr(src, p, "СКОРОСТЬ БЕГА", id("movement_speed"));

                    return 1;
                })
        );
    }

    private static Identifier id(String path) {
        return Identifier.of("minecraft", path);
    }

    private static void sendAttr(ServerCommandSource src, ServerPlayerEntity p, String name, Identifier attrId) {
        EntityAttribute attr = Registries.ATTRIBUTE.get(attrId);
        if (attr == null) {
            src.sendFeedback(() -> Text.literal("§c" + name + ": missing attribute " + attrId), false);
            return;
        }

        RegistryEntry<EntityAttribute> entry = Registries.ATTRIBUTE.getEntry(attr);
        EntityAttributeInstance inst = p.getAttributeInstance(entry);

        if (inst == null) {
            src.sendFeedback(() -> Text.literal("§c" + name + ": n/a"), false);
            return;
        }

        double base = inst.getBaseValue();
        double total = inst.getValue();
        double bonus = total - base;

        src.sendFeedback(() -> Text.literal(String.format(Locale.US,
                "§e%s§7: base=§a%s§7, bonus=§d%s§7, total=§b%s",
                name, fmt(base), fmtSigned(bonus), fmt(total)
        )), false);
    }

    private static String fmt(double v) {
        String s = String.format(Locale.US, "%.4f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    private static String fmtSigned(double v) {
        String s = fmt(Math.abs(v));
        return (v >= 0 ? "+" : "-") + s;
    }
}
