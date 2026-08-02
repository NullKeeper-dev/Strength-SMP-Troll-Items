package dev.nullkeeper.strengthsmptrollitems.items;

import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class TrollItemServiceTest {
    private PluginMock plugin;
    private TrollItemService items;
    private PluginConfig config;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "test");
        items = new TrollItemService(new PersistentKeys(plugin));
        config = new ConfigLoader().load(defaultYaml());
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void createsResizingSwordWithApprovedPresentationAndDurability() {
        ItemStack sword = items.create(TrollItemType.RESIZING_SWORD, config);
        ItemMeta meta = sword.getItemMeta();

        assertEquals(Material.WOODEN_SWORD, sword.getType());
        assertEquals(ChatColor.YELLOW + "" + ChatColor.BOLD + "Resizing Sword", meta.getDisplayName());
        assertEquals(List.of(
                ChatColor.GRAY + "Attack to grow target",
                ChatColor.GRAY + "Sneak + Attack to shrink target"), meta.getLore());
        assertTrue(meta.isUnbreakable());
    }

    @Test
    void createsOrdinaryDurabilityCrossbowAndOrangeBerry() {
        ItemStack crossbow = items.create(TrollItemType.SPOOKY_CROSSBOW, config);
        ItemStack berry = items.create(TrollItemType.HUNGRY_BERRY, config);

        assertEquals(Material.CROSSBOW, crossbow.getType());
        assertFalse(crossbow.getItemMeta().isUnbreakable());
        assertEquals(
                "§x§f§f§a§5§0§0§lHungry Berry",
                berry.getItemMeta().getDisplayName());
    }

    @Test
    void identitySurvivesRenameAndCloneWhileLookalikeStaysVanilla() {
        ItemStack issued = items.create(TrollItemType.RESIZING_SWORD, config);
        ItemMeta renamedMeta = issued.getItemMeta();
        renamedMeta.setDisplayName("Ordinary stick");
        issued.setItemMeta(renamedMeta);
        ItemStack cloned = issued.clone();

        ItemStack lookalike = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta lookalikeMeta = lookalike.getItemMeta();
        lookalikeMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Resizing Sword");
        lookalike.setItemMeta(lookalikeMeta);

        assertTrue(items.isType(issued, TrollItemType.RESIZING_SWORD));
        assertTrue(items.isType(cloned, TrollItemType.RESIZING_SWORD));
        assertFalse(items.isType(lookalike, TrollItemType.RESIZING_SWORD));
    }

    private static YamlConfiguration defaultYaml() {
        InputStream stream = TrollItemServiceTest.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new IllegalStateException("Bundled config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
