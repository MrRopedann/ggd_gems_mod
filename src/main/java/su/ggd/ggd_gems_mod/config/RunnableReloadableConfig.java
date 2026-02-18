package su.ggd.ggd_gems_mod.config;

import java.util.Objects;

public record RunnableReloadableConfig(String id, Runnable loader) implements ReloadableConfig {

    public RunnableReloadableConfig {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(loader, "loader");
    }

    @Override
    public void load() {
        loader.run();
    }
}
