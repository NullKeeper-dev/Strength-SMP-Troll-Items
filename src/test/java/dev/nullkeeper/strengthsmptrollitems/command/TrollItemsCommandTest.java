package dev.nullkeeper.strengthsmptrollitems.command;

import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.ConfigService;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
class TrollItemsCommandTest {
    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock sender;
    private PlayerMock target;
    private TrollItemService items;
    private ConfigService configs;
    private TrollItemsCommand executor;
    private Command command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        sender = server.addPlayer("Admin");
        sender.setOp(false);
        target = server.addPlayer("Target");
        items = new TrollItemService(new PersistentKeys(plugin));
        configs = new ConfigService(new ConfigLoader(), defaultYaml(), TrollItemsCommandTest::defaultYaml);
        executor = new TrollItemsCommand(
                configs,
                new GiveItemService(items),
                plugin.getLogger());
        command = new Command("trollitems") {
            @Override
            public boolean execute(CommandSender commandSender, String label, String[] args) {
                return false;
            }
        };
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void deniesGiveWithoutPermission() {
        execute(sender, "give", target.getName(), "resizing_sword");

        assertEquals(
                ChatColor.RED + "You do not have permission to use this command.",
                sender.nextMessage());
        assertFalse(target.getInventory().contains(Material.WOODEN_SWORD));
    }

    @Test
    void baseHelpShowsOnlyCommandsTheSenderMayUse() {
        sender.addAttachment(plugin, "trollitems.give", true);

        execute(sender);

        assertEquals(
                ChatColor.YELLOW + "Usage: /trollitems give <player> <resizing_sword|spooky_crossbow|hungry_berry> [amount]",
                sender.nextMessage());
        sender.assertNoMoreSaid();
    }

    @Test
    void givesDefaultOneItemAfterValidatingInputs() {
        sender.addAttachment(plugin, "trollitems.give", true);

        execute(sender, "give", target.getName(), "resizing_sword");

        assertTrue(target.getInventory().contains(Material.WOODEN_SWORD));
        assertEquals(
                ChatColor.GREEN + "Gave 1 resizing_sword to Target.",
                sender.nextMessage());
    }

    @Test
    void rejectsOutOfRangeAmountBeforeInventoryChanges() {
        sender.addAttachment(plugin, "trollitems.give", true);

        execute(sender, "give", target.getName(), "hungry_berry", "65");

        assertEquals(
                ChatColor.RED + "Amount must be a whole number from 1 through 64.",
                sender.nextMessage());
        assertFalse(target.getInventory().contains(Material.GLOW_BERRIES));
    }

    @Test
    void deliversUnstackableItemsSeparately() {
        GiveItemService.GiveResult result = new GiveItemService(items)
                .give(target, TrollItemType.SPOOKY_CROSSBOW, 2, configs.current());

        long crossbowSlots = Arrays.stream(target.getInventory().getStorageContents())
                .filter(stack -> stack != null && stack.getType() == Material.CROSSBOW)
                .count();
        assertEquals(2, crossbowSlots);
        assertEquals(2, result.delivered());
        assertEquals(0, result.dropped());
    }

    @Test
    void dropsOverflowAtTargetLocationAndReportsCount() {
        for (int slot = 0; slot < target.getInventory().getSize(); slot++) {
            target.getInventory().setItem(slot, new ItemStack(Material.STONE, 64));
        }
        assertEquals(-1, target.getInventory().firstEmpty());

        GiveItemService.GiveResult result = new GiveItemService(items)
                .give(target, TrollItemType.HUNGRY_BERRY, 2, configs.current());

        assertEquals(0, result.delivered());
        assertEquals(2, result.dropped());
        assertEquals(1, target.getWorld().getEntitiesByClass(Item.class).size());
    }

    @Test
    void reloadCommandAtomicallyAppliesValidDiskSnapshot() {
        sender.addAttachment(plugin, "trollitems.reload", true);
        AtomicReference<YamlConfiguration> disk = new AtomicReference<>(defaultYaml());
        disk.get().set("resize.step", 0.1);
        configs = new ConfigService(new ConfigLoader(), defaultYaml(), disk::get);
        executor = new TrollItemsCommand(configs, new GiveItemService(items), plugin.getLogger());

        execute(sender, "reload");

        assertEquals(0.1, configs.current().resize().step());
        assertEquals(
                ChatColor.GREEN + "Strength SMP Troll Items configuration reloaded.",
                sender.nextMessage());
    }

    private void execute(CommandSender commandSender, String... args) {
        executor.onCommand(commandSender, command, "trollitems", args);
    }

    private static YamlConfiguration defaultYaml() {
        InputStream stream = TrollItemsCommandTest.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new IllegalStateException("Bundled config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
