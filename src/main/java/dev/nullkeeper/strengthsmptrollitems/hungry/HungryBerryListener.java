package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import dev.nullkeeper.strengthsmptrollitems.text.LegacyText;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public final class HungryBerryListener implements Listener {
    private final TrollItemService items;
    private final EdibleItemService edibles;
    private final Supplier<PluginConfig> configSource;

    public HungryBerryListener(
            TrollItemService items,
            EdibleItemService edibles,
            Supplier<PluginConfig> configSource) {
        this.items = Objects.requireNonNull(items, "items");
        this.edibles = Objects.requireNonNull(edibles, "edibles");
        this.configSource = Objects.requireNonNull(configSource, "configSource");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()
                || event.getFinalDamage() <= 0.0
                || !(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof Player target)
                || !items.isType(
                        attacker.getInventory().getItemInMainHand(),
                        TrollItemType.HUNGRY_BERRY)) {
            return;
        }

        ItemStack targetItem = target.getInventory().getItemInMainHand();
        if (targetItem.getType().isAir()) {
            attacker.sendMessage(LegacyText.format(
                    configSource.get().messages().emptyHand(),
                    Map.of("target", target.getName())));
            return;
        }
        target.getInventory().setItemInMainHand(edibles.convert(targetItem));
    }
}
