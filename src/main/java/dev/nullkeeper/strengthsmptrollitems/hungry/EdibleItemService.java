package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.inventory.ItemStack;

public final class EdibleItemService {
    private final TrollItemService items;
    private final Supplier<EdibleSettings> settingsSource;

    public EdibleItemService(
            TrollItemService items,
            Supplier<EdibleSettings> settingsSource) {
        this.items = Objects.requireNonNull(items, "items");
        this.settingsSource = Objects.requireNonNull(settingsSource, "settingsSource");
    }

    public ItemStack convert(ItemStack original) {
        return prepareForUse(items.markEdible(original));
    }

    public ItemStack prepareForUse(ItemStack marked) {
        Objects.requireNonNull(marked, "marked");
        if (!items.isEdible(marked)) {
            throw new IllegalArgumentException("Only marked edible stacks can be prepared");
        }

        EdibleSettings settings = settingsSource.get();
        ItemStack prepared = marked.clone();
        prepared.setData(
                DataComponentTypes.FOOD,
                FoodProperties.food()
                        .nutrition(settings.nutrition())
                        .saturation(settings.saturation())
                        .canAlwaysEat(true));
        prepared.setData(
                DataComponentTypes.CONSUMABLE,
                Consumable.consumable()
                        .consumeSeconds(settings.consumeDelayTicks() / 20.0f)
                        .animation(ItemUseAnimation.EAT));
        return prepared;
    }
}
