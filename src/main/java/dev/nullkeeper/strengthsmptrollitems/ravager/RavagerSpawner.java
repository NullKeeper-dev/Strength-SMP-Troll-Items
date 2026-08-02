package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.RavagerSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class RavagerSpawner {
    private static final int VERTICAL_SEARCH = 3;

    private final Plugin plugin;
    private final RavagerMetadataStore metadata;
    private final PrivateRavagerRegistry registry;

    public RavagerSpawner(
            Plugin plugin,
            RavagerMetadataStore metadata,
            PrivateRavagerRegistry registry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public SpawnResult spawn(Player shooter, Player target, RavagerSettings settings) {
        Objects.requireNonNull(shooter, "shooter");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(settings, "settings");
        List<Location> positions = findPositions(target.getLocation(), settings.spawnRadius());
        RavagerAssignment assignment = new RavagerAssignment(
                shooter.getUniqueId(),
                target.getUniqueId());
        int spawned = 0;
        for (int index = 0; index < settings.perHit() && !positions.isEmpty(); index++) {
            if (spawnOne(positions.get(index % positions.size()), settings, assignment)) {
                spawned++;
            }
        }
        return new SpawnResult(settings.perHit(), spawned);
    }

    private boolean spawnOne(
            Location location,
            RavagerSettings settings,
            RavagerAssignment assignment) {
        try {
            Ravager ravager = location.getWorld().spawn(
                    location,
                    Ravager.class,
                    SpawnReason.CUSTOM,
                    true,
                    spawned -> configure(spawned, settings));
            metadata.write(ravager, assignment);
            registry.register(ravager);
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Could not spawn a private Ravager at " + compact(location),
                    exception);
            return false;
        }
    }

    private static void configure(Ravager ravager, RavagerSettings settings) {
        ravager.setPersistent(true);
        ravager.setRemoveWhenFarAway(false);
        ravager.setCollidable(false);
        ravager.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                PotionEffect.INFINITE_DURATION,
                settings.speedLevel() - 1,
                false,
                false));
    }

    private static List<Location> findPositions(Location center, double radius) {
        World world = center.getWorld();
        int limit = (int) Math.ceil(radius);
        List<Location> positions = new ArrayList<>();
        for (int x = -limit; x <= limit; x++) {
            for (int z = -limit; z <= limit; z++) {
                if ((x * x) + (z * z) > radius * radius) {
                    continue;
                }
                findGround(world, center.getBlockX() + x, center.getBlockY(), center.getBlockZ() + z)
                        .ifPresent(positions::add);
            }
        }
        positions.sort(Comparator.comparingDouble(center::distanceSquared));
        return List.copyOf(positions);
    }

    private static java.util.Optional<Location> findGround(World world, int x, int baseY, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return java.util.Optional.empty();
        }
        for (int offset = VERTICAL_SEARCH; offset >= -VERTICAL_SEARCH; offset--) {
            int y = baseY + offset;
            Block ground = world.getBlockAt(x, y - 1, z);
            if (ground.getType().isSolid()
                    && world.getBlockAt(x, y, z).isPassable()
                    && world.getBlockAt(x, y + 1, z).isPassable()
                    && world.getBlockAt(x, y + 2, z).isPassable()) {
                return java.util.Optional.of(new Location(world, x + 0.5, y, z + 0.5));
            }
        }
        return java.util.Optional.empty();
    }

    private static String compact(Location location) {
        return location.getWorld().getName() + " "
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    public record SpawnResult(int requested, int spawned) {}
}
