package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.RavagerSettings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.Plugin;

public final class RavagerSpawner {
    private static final int VERTICAL_SEARCH = 3;

    private final Plugin plugin;
    private final RavagerMetadataStore metadata;
    private final PrivateRavagerRegistry registry;
    private final Consumer<Ravager> afterRegistration;
    private final RavagerInitializer initializer;
    private final SpawnOperation spawnOperation;

    public RavagerSpawner(
            Plugin plugin,
            RavagerMetadataStore metadata,
            PrivateRavagerRegistry registry) {
        this(plugin, metadata, registry, ravager -> {});
    }

    public RavagerSpawner(
            Plugin plugin,
            RavagerMetadataStore metadata,
            PrivateRavagerRegistry registry,
            Consumer<Ravager> afterRegistration) {
        this(
                plugin,
                metadata,
                registry,
                afterRegistration,
                RavagerInitializer.paper(),
                RavagerSpawner::spawnCustom);
    }

    RavagerSpawner(
            Plugin plugin,
            RavagerMetadataStore metadata,
            PrivateRavagerRegistry registry,
            Consumer<Ravager> afterRegistration,
            RavagerInitializer initializer,
            SpawnOperation spawnOperation) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.afterRegistration = Objects.requireNonNull(afterRegistration, "afterRegistration");
        this.initializer = Objects.requireNonNull(initializer, "initializer");
        this.spawnOperation = Objects.requireNonNull(spawnOperation, "spawnOperation");
    }

    public SpawnResult spawn(UUID shooterId, Player target, RavagerSettings settings) {
        Objects.requireNonNull(shooterId, "shooterId");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(settings, "settings");
        List<Location> positions = findPositions(target.getLocation(), settings.spawnRadius());
        RavagerAssignment assignment = new RavagerAssignment(
                shooterId,
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
        Ravager ravager = null;
        try {
            ravager = spawnOperation.spawn(
                    location,
                    spawned -> initializer.initialize(spawned, settings));
            metadata.write(ravager, assignment);
            registry.register(ravager);
            afterRegistration.accept(ravager);
            return true;
        } catch (RuntimeException exception) {
            if (ravager != null) {
                registry.unregister(ravager.getUniqueId());
                ravager.remove();
            }
            plugin.getLogger().log(
                    Level.WARNING,
                    "Could not spawn a private Ravager at " + compact(location),
                    exception);
            return false;
        }
    }

    private static Ravager spawnCustom(Location location, Consumer<Ravager> initializer) {
        return location.getWorld().spawn(
                location,
                Ravager.class,
                SpawnReason.CUSTOM,
                true,
                initializer);
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
                    && world.getBlockAt(x, y, z).getType().isAir()
                    && world.getBlockAt(x, y + 1, z).getType().isAir()
                    && world.getBlockAt(x, y + 2, z).getType().isAir()) {
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

    @FunctionalInterface
    interface SpawnOperation {
        Ravager spawn(Location location, Consumer<Ravager> initializer);
    }
}
