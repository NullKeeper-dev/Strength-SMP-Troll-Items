package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

public final class EdibleInteractionListener implements Listener {
    private final Plugin plugin;
    private final TrollItemService items;
    private final EdibleItemService edibles;
    private final Supplier<EdibleSettings> settingsSource;
    private final AtomicReference<Set<PendingConsumption>> pending =
            new AtomicReference<>(Set.of());

    public EdibleInteractionListener(
            Plugin plugin,
            TrollItemService items,
            EdibleItemService edibles,
            Supplier<EdibleSettings> settingsSource) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.items = Objects.requireNonNull(items, "items");
        this.edibles = Objects.requireNonNull(edibles, "edibles");
        this.settingsSource = Objects.requireNonNull(settingsSource, "settingsSource");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        if ((event.getAction() != Action.RIGHT_CLICK_AIR
                        && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || hand == null
                || (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND)
                || !items.isEdible(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        int delay = settingsSource.get().consumeDelayTicks();
        if (delay == 0) {
            consumeImmediately(event.getPlayer(), hand);
            return;
        }
        queue(event.getPlayer(), hand, delay);
    }

    private void consumeImmediately(Player player, EquipmentSlot hand) {
        PendingConsumption key = new PendingConsumption(player.getUniqueId(), hand);
        if (!addPending(key)) {
            return;
        }
        boolean consumed;
        try {
            consumed = edibles.consume(player, hand);
        } catch (RuntimeException exception) {
            removePending(key);
            throw exception;
        }
        if (!consumed) {
            removePending(key);
            return;
        }
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> release(key));
        } catch (RuntimeException exception) {
            removePending(key);
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not schedule edible item click-guard cleanup",
                    exception);
        }
    }

    private void queue(Player player, EquipmentSlot hand, int delay) {
        PendingConsumption key = new PendingConsumption(player.getUniqueId(), hand);
        if (!addPending(key)) {
            return;
        }
        try {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> complete(key),
                    delay);
        } catch (RuntimeException exception) {
            removePending(key);
            plugin.getLogger().log(Level.SEVERE, "Could not schedule edible item consumption", exception);
        }
    }

    private void complete(PendingConsumption key) {
        try {
            Player player = plugin.getServer().getPlayer(key.playerId());
            if (player != null) {
                edibles.consume(player, key.hand());
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not consume delayed edible item for " + key.playerId(),
                    exception);
        } finally {
            removePending(key);
        }
    }

    private boolean addPending(PendingConsumption key) {
        while (true) {
            Set<PendingConsumption> current = pending.get();
            if (current.stream().anyMatch(existing ->
                    existing.playerId().equals(key.playerId()))) {
                return false;
            }
            Set<PendingConsumption> changed = new HashSet<>(current);
            changed.add(key);
            if (pending.compareAndSet(current, Set.copyOf(changed))) {
                return true;
            }
        }
    }

    private void release(PendingConsumption key) {
        try {
            removePending(key);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not release edible item click guard for " + key.playerId(),
                    exception);
        }
    }

    private void removePending(PendingConsumption key) {
        while (true) {
            Set<PendingConsumption> current = pending.get();
            if (!current.contains(key)) {
                return;
            }
            Set<PendingConsumption> changed = new HashSet<>(current);
            changed.remove(key);
            if (pending.compareAndSet(current, Set.copyOf(changed))) {
                return;
            }
        }
    }

    private record PendingConsumption(UUID playerId, EquipmentSlot hand) {}
}
