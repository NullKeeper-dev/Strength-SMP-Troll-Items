package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.plugin.Plugin;

public final class RavagerTargetController implements Runnable {
    private final Plugin plugin;
    private final PrivateRavagerRegistry registry;
    private final RavagerMetadataStore metadata;

    public RavagerTargetController(
            Plugin plugin,
            PrivateRavagerRegistry registry,
            RavagerMetadataStore metadata) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    @Override
    public void run() {
        for (Ravager ravager : registry.snapshot().values()) {
            try {
                updateTarget(ravager);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Could not update private Ravager target for " + ravager.getUniqueId(),
                        exception);
            }
        }
    }

    private void updateTarget(Ravager ravager) {
        if (ravager.isDead() || !ravager.isValid()) {
            registry.unregister(ravager.getUniqueId());
            return;
        }
        Optional<RavagerAssignment> stored = metadata.read(ravager);
        if (stored.isEmpty()) {
            registry.unregister(ravager.getUniqueId());
            return;
        }

        Player target = Bukkit.getPlayer(stored.get().targetId());
        if (eligible(ravager, target)) {
            ravager.setTarget(target);
        } else if (ravager.getTarget() instanceof Player) {
            ravager.setTarget(null);
        }
    }

    private static boolean eligible(Ravager ravager, Player target) {
        AttributeInstance followRange = ravager.getAttribute(Attribute.FOLLOW_RANGE);
        if (target == null
                || !target.isOnline()
                || target.isDead()
                || target.getWorld() != ravager.getWorld()
                || followRange == null) {
            return false;
        }
        double range = followRange.getValue();
        return ravager.getLocation().distanceSquared(target.getLocation()) <= range * range;
    }
}
