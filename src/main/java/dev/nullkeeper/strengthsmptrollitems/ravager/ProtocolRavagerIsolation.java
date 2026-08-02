package dev.nullkeeper.strengthsmptrollitems.ravager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.plugin.Plugin;

public final class ProtocolRavagerIsolation implements AutoCloseable {
    private final Plugin plugin;
    private final ProtocolManager manager;
    private final PrivateRavagerRegistry registry;
    private final RavagerMetadataStore metadata;
    private final RavagerAccessPolicy policy;
    private boolean started;

    public ProtocolRavagerIsolation(
            Plugin plugin,
            ProtocolManager manager,
            PrivateRavagerRegistry registry,
            RavagerMetadataStore metadata,
            RavagerAccessPolicy policy) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.manager = Objects.requireNonNull(manager, "manager");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public void start() {
        if (started) {
            return;
        }
        try {
            manager.addPacketListener(soundFilter());
            manager.addPacketListener(interactionFilter());
            started = true;
        } catch (RuntimeException exception) {
            manager.removePacketListeners(plugin);
            throw new IllegalStateException(
                    "ProtocolLib could not register private Ravager packet filters",
                    exception);
        }
    }

    @Override
    public void close() {
        if (started) {
            manager.removePacketListeners(plugin);
            started = false;
        }
    }

    private PacketAdapter soundFilter() {
        return new PacketAdapter(
                plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.ENTITY_SOUND) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    Entity entity = event.getPacket().getEntityModifier(event).readSafely(0);
                    if (observationDenied(event.getPlayer(), entity)) {
                        event.setCancelled(true);
                    }
                } catch (RuntimeException exception) {
                    failClosed(event, "outbound entity sound", exception);
                }
            }
        };
    }

    private PacketAdapter interactionFilter() {
        return new PacketAdapter(
                plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Client.USE_ENTITY) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                try {
                    Integer entityId = event.getPacket().getIntegers().readSafely(0);
                    Entity entity = entityId == null
                            ? null
                            : manager.getEntityFromID(event.getPlayer().getWorld(), entityId);
                    if (interactionDenied(event.getPlayer(), entity)) {
                        event.setCancelled(true);
                    }
                } catch (RuntimeException exception) {
                    failClosed(event, "inbound entity interaction", exception);
                }
            }
        };
    }

    private boolean observationDenied(Player viewer, Entity entity) {
        Optional<RavagerAssignment> assignment = privateAssignment(entity);
        return assignment.isPresent()
                && !policy.canObserve(viewer.getUniqueId(), assignment.get());
    }

    private boolean interactionDenied(Player viewer, Entity entity) {
        Optional<RavagerAssignment> assignment = privateAssignment(entity);
        return assignment.isPresent()
                && !policy.canInteract(viewer.getUniqueId(), assignment.get());
    }

    private Optional<RavagerAssignment> privateAssignment(Entity entity) {
        if (!(entity instanceof Ravager ravager)
                || registry.find(ravager.getUniqueId()).isEmpty()) {
            return Optional.empty();
        }
        return metadata.read(ravager);
    }

    private void failClosed(PacketEvent event, String direction, RuntimeException exception) {
        event.setCancelled(true);
        plugin.getLogger().log(
                Level.WARNING,
                "Blocked " + direction + " after a ProtocolLib decode failure",
                exception);
    }
}
