package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
class EdibleItemServiceTest {
    private PluginMock plugin;
    private TrollItemService trollItems;
    private EdibleItemService service;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "test");
        trollItems = new TrollItemService(new PersistentKeys(plugin));
        service = new EdibleItemService(trollItems, () -> new EdibleSettings(0, 0.0f, 6));
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
        assertNativeComponents(converted, 0, 0.0f, 0.3f);
    }

    @Test
    void configuredValuesBecomeNativeComponentValues() {
        EdibleItemService configured = new EdibleItemService(
                trollItems,
                () -> new EdibleSettings(4, 1.5f, 10));

        ItemStack converted = configured.convert(new ItemStack(Material.STONE, 3));

        assertNativeComponents(converted, 4, 1.5f, 0.5f);
    }

    @Test
    void prepareForUseUpgradesLegacyMarkedStackWithoutMutatingIt() {
        ItemStack legacy = trollItems.markEdible(new ItemStack(Material.SHIELD, 2));

        ItemStack prepared = service.prepareForUse(legacy);

        assertNotSame(legacy, prepared);
        assertFalse(legacy.hasData(DataComponentTypes.FOOD));
        assertFalse(legacy.hasData(DataComponentTypes.CONSUMABLE));
        assertTrue(trollItems.isEdible(prepared));
        assertEquals(2, prepared.getAmount());
        assertNativeComponents(prepared, 0, 0.0f, 0.3f);
    }

    @Test
    void prepareForUseRejectsUnmarkedStack() {
        ItemStack unmarked = new ItemStack(Material.STONE);

        assertThrows(IllegalArgumentException.class, () -> service.prepareForUse(unmarked));
    }

    private static void assertNativeComponents(
            ItemStack stack,
            int nutrition,
            float saturation,
            float consumeSeconds) {
        FoodProperties food = stack.getData(DataComponentTypes.FOOD);
        Consumable consumable = stack.getData(DataComponentTypes.CONSUMABLE);
        assertNotNull(food);
        assertEquals(nutrition, food.nutrition());
        assertEquals(saturation, food.saturation());
        assertTrue(food.canAlwaysEat());
        assertNotNull(consumable);
        assertEquals(consumeSeconds, consumable.consumeSeconds(), 0.0001f);
        assertEquals(ItemUseAnimation.EAT, consumable.animation());
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
