package dev.nullkeeper.strengthsmptrollitems.command;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class GiveItemService {
    private final TrollItemService items;

    public GiveItemService(TrollItemService items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    public GiveResult give(
            Player target,
            TrollItemType type,
            int amount,
            PluginConfig config) {
        List<ItemStack> requested = createRequestedStacks(type, amount, config);
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(
                requested.toArray(ItemStack[]::new));
        int dropped = overflow.values().stream().mapToInt(ItemStack::getAmount).sum();
        overflow.values().forEach(stack ->
                target.getWorld().dropItemNaturally(target.getLocation(), stack.clone()));
        return new GiveResult(amount - dropped, dropped);
    }

    private List<ItemStack> createRequestedStacks(
            TrollItemType type,
            int amount,
            PluginConfig config) {
        ItemStack template = items.create(type, config);
        if (template.getMaxStackSize() > 1) {
            template.setAmount(amount);
            return List.of(template);
        }
        List<ItemStack> stacks = new ArrayList<>(amount);
        for (int index = 0; index < amount; index++) {
            stacks.add(template.clone());
        }
        return List.copyOf(stacks);
    }

    public record GiveResult(int delivered, int dropped) {}
}
