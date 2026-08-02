package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
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
        return items.markEdible(original);
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
