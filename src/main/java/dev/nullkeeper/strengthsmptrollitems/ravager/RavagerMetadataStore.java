package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Ravager;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class RavagerMetadataStore {
    private final PersistentKeys keys;

    public RavagerMetadataStore(PersistentKeys keys) {
        this.keys = Objects.requireNonNull(keys, "keys");
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
        String shooter = data.get(keys.ravagerShooter(), PersistentDataType.STRING);
        String target = data.get(keys.ravagerTarget(), PersistentDataType.STRING);
        if (shooter == null || target == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new RavagerAssignment(
                    UUID.fromString(shooter),
                    UUID.fromString(target)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
