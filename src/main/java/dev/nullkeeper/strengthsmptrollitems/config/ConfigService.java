package dev.nullkeeper.strengthsmptrollitems.config;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.bukkit.configuration.ConfigurationSection;

public final class ConfigService {
    private final ConfigLoader loader;
    private final Supplier<? extends ConfigurationSection> diskSource;
    private final AtomicReference<PluginConfig> current;

    public ConfigService(
            ConfigLoader loader,
            ConfigurationSection initial,
            Supplier<? extends ConfigurationSection> diskSource) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.diskSource = Objects.requireNonNull(diskSource, "diskSource");
        this.current = new AtomicReference<>(loader.load(initial));
    }

    public PluginConfig current() {
        return current.get();
    }

    public ReloadResult reload(ConfigurationSection candidate) {
        try {
            PluginConfig loaded = loader.load(candidate);
            current.set(loaded);
            return new ReloadResult(true, "Configuration reloaded");
        } catch (RuntimeException exception) {
            return new ReloadResult(false, safeMessage(exception));
        }
    }

    public ReloadResult reloadFromDisk() {
        try {
            return reload(diskSource.get());
        } catch (RuntimeException exception) {
            return new ReloadResult(false, safeMessage(exception));
        }
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    public record ReloadResult(boolean successful, String message) {}
}
