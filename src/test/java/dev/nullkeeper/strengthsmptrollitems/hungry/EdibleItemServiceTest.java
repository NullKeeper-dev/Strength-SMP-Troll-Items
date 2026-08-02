package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
class EdibleItemServiceTest {
    private PlayerMock player;
    private PluginMock plugin;
    private TrollItemService trollItems;
    private EdibleItemService service;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        player = server.addPlayer("Hungry");
        trollItems = new TrollItemService(new PersistentKeys(plugin));
        service = new EdibleItemService(trollItems, () -> new EdibleSettings(0, 0.0f, 0));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void conversionClonesAndPreservesTheEntireStackMetadata() {
        ItemStack original = detailedStack();

        ItemStack converted = service.convert(original);

        assertNotSame(original, converted);
        assertEquals(original.getType(), converted.getType());
        assertEquals(original.getAmount(), converted.getAmount());
        assertEquals(((Damageable) original.getItemMeta()).getDamage(),
                ((Damageable) converted.getItemMeta()).getDamage());
        assertEquals(original.getEnchantments(), converted.getEnchantments());
        assertEquals(original.getItemMeta().getDisplayName(), converted.getItemMeta().getDisplayName());
        assertEquals(original.getItemMeta().getLore(), converted.getItemMeta().getLore());
        assertEquals(original.getItemMeta().getCustomModelData(),
                converted.getItemMeta().getCustomModelData());
        NamespacedKey existing = new NamespacedKey(plugin, "existing");
        assertEquals("kept", converted.getItemMeta().getPersistentDataContainer().get(
                existing,
                PersistentDataType.STRING));
        assertFalse(trollItems.isEdible(original));
        assertTrue(trollItems.isEdible(converted));
    }

    @Test
    void consumeRemovesOneAtFullHungerWithoutChangingFoodValues() {
        player.setFoodLevel(20);
        player.setSaturation(7.0f);
        ItemStack converted = service.convert(new ItemStack(Material.STONE, 3));
        player.getInventory().setItemInMainHand(converted);

        boolean consumed = service.consume(player, EquipmentSlot.HAND);

        assertTrue(consumed);
        assertEquals(2, player.getInventory().getItemInMainHand().getAmount());
        assertTrue(trollItems.isEdible(player.getInventory().getItemInMainHand()));
        assertEquals(20, player.getFoodLevel());
        assertEquals(7.0f, player.getSaturation());
    }

    @Test
    void consumeFinalOffhandItemReplacesItWithAir() {
        player.getInventory().setItemInOffHand(service.convert(new ItemStack(Material.SHIELD)));

        boolean consumed = service.consume(player, EquipmentSlot.OFF_HAND);

        assertTrue(consumed);
        assertTrue(player.getInventory().getItemInOffHand().getType().isAir());
    }

    @Test
    void consumeRejectsUnmarkedItems() {
        player.getInventory().setItemInMainHand(new ItemStack(Material.STONE, 3));

        assertFalse(service.consume(player, EquipmentSlot.HAND));
        assertEquals(3, player.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void configuredNutritionAndSaturationStayInsideVanillaBounds() {
        EdibleItemService nourishing = new EdibleItemService(
                trollItems,
                () -> new EdibleSettings(20, 100.0f, 0));
        player.setFoodLevel(1);
        player.setSaturation(0.0f);
        player.getInventory().setItemInMainHand(
                nourishing.convert(new ItemStack(Material.STONE)));

        nourishing.consume(player, EquipmentSlot.HAND);

        assertEquals(20, player.getFoodLevel());
        assertEquals(20.0f, player.getSaturation());
    }

    private ItemStack detailedStack() {
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD, 4);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("Named Blade");
        meta.setLore(List.of("Old lore"));
        meta.setCustomModelData(42);
        ((Damageable) meta).setDamage(17);
        meta.addEnchant(Enchantment.UNBREAKING, 2, true);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "existing"),
                PersistentDataType.STRING,
                "kept");
        stack.setItemMeta(meta);
        return stack;
    }
}
