package dev.nullkeeper.strengthsmptrollitems.ravager;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class RavagerProtectionListener implements Listener {
    private final RavagerMetadataStore metadata;
    private final RavagerAccessPolicy policy;

    public RavagerProtectionListener(
            RavagerMetadataStore metadata,
            RavagerAccessPolicy policy) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Ravager ravager) {
            Optional<UUID> attacker = playerId(event.getDamager());
            metadata.read(ravager).ifPresent(assignment -> {
                if (attacker.isPresent()
                        && !policy.canBeDamagedBy(attacker.get(), assignment)) {
                    event.setCancelled(true);
                }
            });
        }
        if (event.getDamager() instanceof Ravager ravager
                && event.getEntity() instanceof Player victim) {
            metadata.read(ravager).ifPresent(assignment -> {
                if (!policy.canDamagePlayer(victim.getUniqueId(), assignment)) {
                    event.setCancelled(true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Ravager ravager
                && event.getTarget() instanceof Player target) {
            metadata.read(ravager).ifPresent(assignment -> {
                if (!policy.canDamagePlayer(target.getUniqueId(), assignment)) {
                    event.setCancelled(true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Ravager ravager) {
            metadata.read(ravager).ifPresent(assignment -> {
                if (!policy.canInteract(event.getPlayer().getUniqueId(), assignment)) {
                    event.setCancelled(true);
                }
            });
        }
    }

    private static Optional<UUID> playerId(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) {
            return Optional.of(player.getUniqueId());
        }
        if (damager instanceof Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return Optional.of(player.getUniqueId());
        }
        return Optional.empty();
    }
}
