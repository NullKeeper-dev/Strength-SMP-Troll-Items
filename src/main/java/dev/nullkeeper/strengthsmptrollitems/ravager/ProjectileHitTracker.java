package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ProjectileHitTracker {
    private final PersistentKeys keys;
    private final Consumer<String> warningSink;

    public ProjectileHitTracker(PersistentKeys keys) {
        this(keys, ignored -> {});
    }

    public ProjectileHitTracker(PersistentKeys keys, Consumer<String> warningSink) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
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
        PersistentDataContainer data = projectile.getPersistentDataContainer();
        String stored = data.get(
                keys.projectileShooter(),
                PersistentDataType.STRING);
        if (stored == null) {
            if (data.has(keys.projectileShooter())) {
                warningSink.accept("Invalid Spooky Crossbow shooter type on projectile "
                        + projectile.getUniqueId());
            }
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(stored));
        } catch (IllegalArgumentException exception) {
            warningSink.accept("Invalid Spooky Crossbow shooter UUID on projectile "
                    + projectile.getUniqueId());
            return Optional.empty();
        }
    }

    public boolean markFirst(Projectile projectile, UUID victimId) {
        Objects.requireNonNull(projectile, "projectile");
        Objects.requireNonNull(victimId, "victimId");
        PersistentDataContainer data = projectile.getPersistentDataContainer();
        String storedVictims = data.get(
                keys.projectileVictims(),
                PersistentDataType.STRING);
        if (storedVictims == null && data.has(keys.projectileVictims())) {
            warningSink.accept("Invalid Spooky Crossbow victim history type on projectile "
                    + projectile.getUniqueId());
        }
        Set<UUID> victims = parse(projectile, storedVictims);
        if (!victims.add(victimId)) {
            return false;
        }
        data.set(
                keys.projectileVictims(),
                PersistentDataType.STRING,
                victims.stream().map(UUID::toString).collect(Collectors.joining(",")));
        return true;
    }

    private Set<UUID> parse(Projectile projectile, String stored) {
        Set<UUID> victims = new LinkedHashSet<>();
        if (stored == null || stored.isBlank()) {
            return victims;
        }
        for (String part : stored.split(",")) {
            try {
                victims.add(UUID.fromString(part));
            } catch (IllegalArgumentException ignored) {
                warningSink.accept("Invalid Spooky Crossbow victim UUID on projectile "
                        + projectile.getUniqueId());
            }
        }
        return victims;
    }
}
