package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

public final class RavagerLifecycleListener implements Listener {
    private final Plugin plugin;
    private final PrivateRavagerRegistry registry;
    private final RavagerMetadataStore metadata;
    private final RavagerVisibilityService visibility;

    public RavagerLifecycleListener(
            Plugin plugin,
            PrivateRavagerRegistry registry,
            RavagerMetadataStore metadata,
            RavagerVisibilityService visibility) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.visibility = Objects.requireNonNull(visibility, "visibility");
    }

    public void scanLoadedWorlds() {
        for (World world : Bukkit.getWorlds()) {
            for (Ravager ravager : world.getEntitiesByClass(Ravager.class)) {
                registerIfMarked(ravager);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Ravager ravager) {
                registerIfMarked(ravager);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Ravager ravager) {
                registry.unregister(ravager.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        visibility.refresh(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        visibility.refresh(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                visibility.refresh(event.getPlayer());
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Could not refresh Ravager visibility after respawn for "
                                + event.getPlayer().getUniqueId(),
                        exception);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Ravager ravager : registry.snapshot().values()) {
            if (ravager.getTarget() == player) {
                ravager.setTarget(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Ravager ravager) {
            registry.unregister(ravager.getUniqueId());
        }
    }

    private void registerIfMarked(Ravager ravager) {
        if (metadata.read(ravager).isPresent()) {
            registry.register(ravager);
            visibility.refresh(ravager);
        }
    }
}
