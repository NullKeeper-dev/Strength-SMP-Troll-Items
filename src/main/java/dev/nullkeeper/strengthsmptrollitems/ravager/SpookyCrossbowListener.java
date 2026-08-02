package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerSpawner.SpawnResult;
import dev.nullkeeper.strengthsmptrollitems.text.LegacyText;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public final class SpookyCrossbowListener implements Listener {
    private final TrollItemService items;
    private final ProjectileHitTracker tracker;
    private final RavagerSpawner spawner;
    private final Supplier<PluginConfig> configSource;

    public SpookyCrossbowListener(
            TrollItemService items,
            ProjectileHitTracker tracker,
            RavagerSpawner spawner,
            Supplier<PluginConfig> configSource) {
        this.items = Objects.requireNonNull(items, "items");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.spawner = Objects.requireNonNull(spawner, "spawner");
        this.configSource = Objects.requireNonNull(configSource, "configSource");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player shooter
                && event.getProjectile() instanceof Projectile projectile
                && items.isType(event.getBow(), TrollItemType.SPOOKY_CROSSBOW)) {
            tracker.tag(projectile, shooter.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()
                || event.getFinalDamage() <= 0.0
                || !(event.getDamager() instanceof Projectile projectile)
                || !(event.getEntity() instanceof Player target)) {
            return;
        }
        UUID shooterId = tracker.shooterId(projectile).orElse(null);
        if (shooterId == null || !tracker.markFirst(projectile, target.getUniqueId())) {
            return;
        }
        Player shooter = Bukkit.getPlayer(shooterId);
        if (shooter == null) {
            return;
        }

        PluginConfig config = configSource.get();
        SpawnResult result = spawner.spawn(shooter, target, config.ravagers());
        if (result.spawned() < result.requested()) {
            shooter.sendMessage(LegacyText.format(
                    config.messages().partialRavagerSpawn(),
                    Map.of(
                            "spawned", Integer.toString(result.spawned()),
                            "requested", Integer.toString(result.requested()))));
        }
    }
}
