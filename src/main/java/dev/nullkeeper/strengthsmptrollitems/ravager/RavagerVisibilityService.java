package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.plugin.Plugin;

public final class RavagerVisibilityService {
    private final Plugin plugin;
    private final PrivateRavagerRegistry registry;
    private final RavagerMetadataStore metadata;
    private final RavagerAccessPolicy policy;

    public RavagerVisibilityService(
            Plugin plugin,
            PrivateRavagerRegistry registry,
            RavagerMetadataStore metadata,
            RavagerAccessPolicy policy) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public void refresh(Ravager ravager) {
        Optional<RavagerAssignment> assignment = metadata.read(ravager);
        if (assignment.isEmpty()) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            apply(viewer, ravager, assignment.get());
        }
    }

    public void refresh(Player viewer) {
        for (Ravager ravager : registry.snapshot().values()) {
            metadata.read(ravager).ifPresent(assignment -> apply(viewer, ravager, assignment));
        }
    }

    public void hideFromUnauthorized(Ravager ravager) {
        metadata.read(ravager).ifPresent(assignment -> {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!policy.canObserve(viewer.getUniqueId(), assignment)) {
                    viewer.hideEntity(plugin, ravager);
                }
            }
        });
    }

    private void apply(Player viewer, Ravager ravager, RavagerAssignment assignment) {
        if (policy.canObserve(viewer.getUniqueId(), assignment)) {
            viewer.showEntity(plugin, ravager);
        } else {
            viewer.hideEntity(plugin, ravager);
        }
    }
}
