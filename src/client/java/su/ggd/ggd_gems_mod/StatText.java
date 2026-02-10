package su.ggd.ggd_gems_mod;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class StatText {
    private StatText() {}

    public static Text displayName(String statId) {
        if (statId == null || statId.isBlank()) return Text.literal("?");

        Identifier id = Identifier.tryParse(statId);
        if (id == null) return Text.literal(statId);

        EntityAttribute attr = Registries.ATTRIBUTE.get(id);
        if (attr == null) return Text.literal(statId);

        return Text.translatable(attr.getTranslationKey());
    }
}
