package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ProjectileHitTracker {
    private final PersistentKeys keys;

    public ProjectileHitTracker(PersistentKeys keys) {
        this.keys = Objects.requireNonNull(keys, "keys");
    }

    public void tag(Projectile projectile, UUID shooterId) {
        Objects.requireNonNull(projectile, "projectile");
        Objects.requireNonNull(shooterId, "shooterId");
        projectile.getPersistentDataContainer().set(
                keys.projectileShooter(),
                PersistentDataType.STRING,
                shooterId.toString());
    }

    public Optional<UUID> shooterId(Projectile projectile) {
        String stored = projectile.getPersistentDataContainer().get(
                keys.projectileShooter(),
                PersistentDataType.STRING);
        if (stored == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(stored));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public boolean markFirst(Projectile projectile, UUID victimId) {
        Objects.requireNonNull(projectile, "projectile");
        Objects.requireNonNull(victimId, "victimId");
        PersistentDataContainer data = projectile.getPersistentDataContainer();
        Set<UUID> victims = parse(data.get(
                keys.projectileVictims(),
                PersistentDataType.STRING));
        if (!victims.add(victimId)) {
            return false;
        }
        data.set(
                keys.projectileVictims(),
                PersistentDataType.STRING,
                victims.stream().map(UUID::toString).collect(Collectors.joining(",")));
        return true;
    }

    private static Set<UUID> parse(String stored) {
        Set<UUID> victims = new LinkedHashSet<>();
        if (stored == null || stored.isBlank()) {
            return victims;
        }
        for (String part : stored.split(",")) {
            try {
                victims.add(UUID.fromString(part));
            } catch (IllegalArgumentException ignored) {
                // Ignore corrupted entries while preserving valid projectile history.
            }
        }
        return victims;
    }
}
