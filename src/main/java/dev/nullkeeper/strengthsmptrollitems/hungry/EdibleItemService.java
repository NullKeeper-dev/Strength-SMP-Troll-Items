package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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

    public boolean consume(Player player, EquipmentSlot slot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(slot, "slot");
        PlayerInventory inventory = player.getInventory();
        ItemStack held = heldItem(inventory, slot);
        if (!items.isEdible(held)) {
            return false;
        }

        ItemStack remaining = held.clone();
        remaining.setAmount(held.getAmount() - 1);
        replace(inventory, slot, remaining.getAmount() == 0
                ? new ItemStack(Material.AIR)
                : remaining);
        applyNutrition(player, settingsSource.get());
        return true;
    }

    private static ItemStack heldItem(PlayerInventory inventory, EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> inventory.getItemInMainHand();
            case OFF_HAND -> inventory.getItemInOffHand();
            default -> new ItemStack(Material.AIR);
        };
    }

    private static void replace(
            PlayerInventory inventory,
            EquipmentSlot slot,
            ItemStack item) {
        switch (slot) {
            case HAND -> inventory.setItemInMainHand(item);
            case OFF_HAND -> inventory.setItemInOffHand(item);
            default -> throw new IllegalArgumentException("Only hand equipment slots can be consumed");
        }
    }

    private static void applyNutrition(Player player, EdibleSettings settings) {
        int food = Math.clamp(player.getFoodLevel() + settings.nutrition(), 0, 20);
        float saturation = Math.clamp(
                player.getSaturation() + settings.saturation(),
                0.0f,
                (float) food);
        player.setFoodLevel(food);
        player.setSaturation(saturation);
    }
}
