package dev.nullkeeper.strengthsmptrollitems.update;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.text.LegacyText;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UpdateNotificationListener implements Listener {
    private static final String PERMISSION = "trollitems.update-notify";

    private final Supplier<PluginConfig> configSource;
    private final Supplier<Optional<UpdateInfo>> updateSource;
    private final AtomicReference<Set<UUID>> notified = new AtomicReference<>(Set.of());

    public UpdateNotificationListener(
            Supplier<PluginConfig> configSource,
            Supplier<Optional<UpdateInfo>> updateSource) {
        this.configSource = Objects.requireNonNull(configSource, "configSource");
        this.updateSource = Objects.requireNonNull(updateSource, "updateSource");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        PluginConfig config = configSource.get();
        Player player = event.getPlayer();
        if (!config.updateChecker().enabled() || !player.hasPermission(PERMISSION)) {
            return;
        }
        Optional<UpdateInfo> update = updateSource.get();
        if (update.isEmpty() || !markNotified(player.getUniqueId())) {
            return;
        }

        UpdateInfo info = update.orElseThrow();
        player.sendMessage(LegacyText.format(
                config.messages().updateAvailable(),
                Map.of(
                        "current", info.current(),
                        "latest", info.latest(),
                        "url", info.pageUrl())));
        player.sendMessage(LegacyText.color(config.messages().updateDisableHint()));
    }

    private boolean markNotified(UUID playerId) {
        while (true) {
            Set<UUID> current = notified.get();
            if (current.contains(playerId)) {
                return false;
            }
            Set<UUID> changed = new HashSet<>(current);
            changed.add(playerId);
            if (notified.compareAndSet(current, Set.copyOf(changed))) {
                return true;
            }
        }
    }
}
