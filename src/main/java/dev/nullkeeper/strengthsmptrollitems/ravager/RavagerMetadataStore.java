package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Ravager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class RavagerMetadataStore {
    private final PersistentKeys keys;
    private final Consumer<String> warningSink;

    public RavagerMetadataStore(PersistentKeys keys) {
        this(keys, ignored -> {});
    }

    public RavagerMetadataStore(PersistentKeys keys, Consumer<String> warningSink) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
    }

    public void write(Ravager ravager, RavagerAssignment assignment) {
        Objects.requireNonNull(ravager, "ravager");
        Objects.requireNonNull(assignment, "assignment");
        PersistentDataContainer data = ravager.getPersistentDataContainer();
        data.set(
                keys.ravagerShooter(),
                PersistentDataType.STRING,
                assignment.shooterId().toString());
        data.set(
                keys.ravagerTarget(),
                PersistentDataType.STRING,
                assignment.targetId().toString());
    }

    public Optional<RavagerAssignment> read(Ravager ravager) {
        Objects.requireNonNull(ravager, "ravager");
        PersistentDataContainer data = ravager.getPersistentDataContainer();
        boolean shooterPresent = data.has(keys.ravagerShooter());
        boolean targetPresent = data.has(keys.ravagerTarget());
        String shooter = data.get(keys.ravagerShooter(), PersistentDataType.STRING);
        String target = data.get(keys.ravagerTarget(), PersistentDataType.STRING);
        if (!shooterPresent && !targetPresent) {
            return Optional.empty();
        }
        if (shooter == null || target == null) {
            warningSink.accept("Invalid or incomplete private Ravager metadata for "
                    + ravager.getUniqueId());
            return Optional.empty();
        }
        try {
            return Optional.of(new RavagerAssignment(
                    UUID.fromString(shooter),
                    UUID.fromString(target)));
        } catch (IllegalArgumentException exception) {
            warningSink.accept("Invalid private Ravager UUID metadata for " + ravager.getUniqueId());
            return Optional.empty();
        }
    }
}
