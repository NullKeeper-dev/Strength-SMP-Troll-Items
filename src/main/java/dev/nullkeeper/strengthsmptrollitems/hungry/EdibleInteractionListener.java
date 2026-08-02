package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class EdibleInteractionListener implements Listener {
    private final TrollItemService items;
    private final EdibleItemService edibles;

    public EdibleInteractionListener(
            TrollItemService items,
            EdibleItemService edibles) {
        this.items = Objects.requireNonNull(items, "items");
        this.edibles = Objects.requireNonNull(edibles, "edibles");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        EquipmentSlot hand = event.getHand();
        if (event.useItemInHand() == Event.Result.DENY
                || !isRightClick(event.getAction())
                || !isHand(hand)
                || !items.isEdible(event.getItem())) {
            return;
        }

        ItemStack prepared = edibles.prepareForUse(event.getItem());
        replace(event.getPlayer().getInventory(), hand, prepared);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.ALLOW);
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    private static boolean isHand(EquipmentSlot slot) {
        return slot == EquipmentSlot.HAND || slot == EquipmentSlot.OFF_HAND;
    }

    private static void replace(
            PlayerInventory inventory,
            EquipmentSlot slot,
            ItemStack item) {
        switch (slot) {
            case HAND -> inventory.setItemInMainHand(item);
            case OFF_HAND -> inventory.setItemInOffHand(item);
            default -> throw new IllegalArgumentException("Only hand equipment slots can be prepared");
        }
    }
}
