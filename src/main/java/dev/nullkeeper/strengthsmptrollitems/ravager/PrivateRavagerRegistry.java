package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Ravager;

public final class PrivateRavagerRegistry {
    private final AtomicReference<Map<UUID, Ravager>> ravagers =
            new AtomicReference<>(Map.of());

    public void register(Ravager ravager) {
        Objects.requireNonNull(ravager, "ravager");
        update(ravager.getUniqueId(), ravager);
    }

    public void unregister(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId");
        update(entityId, null);
    }

    public Optional<Ravager> find(UUID entityId) {
        return Optional.ofNullable(ravagers.get().get(entityId));
    }

    public Map<UUID, Ravager> snapshot() {
        return ravagers.get();
    }

    private void update(UUID entityId, Ravager ravager) {
        while (true) {
            Map<UUID, Ravager> current = ravagers.get();
            Map<UUID, Ravager> changed = new HashMap<>(current);
            if (ravager == null) {
                changed.remove(entityId);
            } else {
                changed.put(entityId, ravager);
            }
            if (ravagers.compareAndSet(current, Map.copyOf(changed))) {
                return;
            }
        }
    }
}
