package dev.nullkeeper.strengthsmptrollitems.resize;

import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

public final class ScalePersistenceListener implements Listener {
    private final Plugin plugin;
    private final ScaleService scales;

    public ScalePersistenceListener(Plugin plugin, ScaleService scales) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scales = Objects.requireNonNull(scales, "scales");
    }

    public void scanLoadedWorlds() {
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                scales.restore(entity);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scales.restore(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                scales.restore(event.getPlayer());
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Could not restore scale after respawn for "
                                + event.getPlayer().getUniqueId(),
                        exception);
            }
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                scales.restore(livingEntity);
            }
        }
    }
}
