package dev.nullkeeper.strengthsmptrollitems.hungry;

import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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
class HungryBerryListenerTest {
    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock attacker;
    private PlayerMock target;
    private TrollItemService items;
    private PluginConfig config;
    private AtomicReference<EdibleSettings> settings;
    private EdibleItemService edibles;
    private HungryBerryListener berryListener;
    private EdibleInteractionListener interactionListener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "test");
        attacker = server.addPlayer("Attacker");
        target = server.addPlayer("Target");
        items = new TrollItemService(new PersistentKeys(plugin));
        config = new ConfigLoader().load(defaultYaml());
        settings = new AtomicReference<>(config.edible());
        edibles = new EdibleItemService(items, settings::get);
        berryListener = new HungryBerryListener(items, edibles, () -> config);
        interactionListener = new EdibleInteractionListener(
                plugin,
                items,
                edibles,
                settings::get);
        attacker.getInventory().setItemInMainHand(items.create(TrollItemType.HUNGRY_BERRY, config));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void hitConvertsOnlyTargetsCompleteMainHandStack() {
        target.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 12));
        target.getInventory().setItemInOffHand(new ItemStack(Material.EMERALD, 4));

        berryListener.onDamage(damage(attacker, target, 1.0));

        ItemStack converted = target.getInventory().getItemInMainHand();
        assertEquals(12, converted.getAmount());
        assertTrue(items.isEdible(converted));
        assertFalse(items.isEdible(target.getInventory().getItemInOffHand()));
        assertEquals(1, attacker.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void emptyTargetHandReportsMessage() {
        target.getInventory().setItemInMainHand(null);

        berryListener.onDamage(damage(attacker, target, 1.0));

        assertEquals(
                ChatColor.YELLOW + "Target is not holding an item in their main hand.",
                attacker.nextMessage());
    }

    @Test
    void cancelledZeroDamageOrNonPlayerTargetsDoNothing() {
        ItemStack held = new ItemStack(Material.DIAMOND, 2);
        target.getInventory().setItemInMainHand(held);
        EntityDamageByEntityEvent cancelled = damage(attacker, target, 1.0);
        cancelled.setCancelled(true);

        berryListener.onDamage(cancelled);
        berryListener.onDamage(damage(attacker, target, 0.0));
        LivingEntity cow = (LivingEntity) target.getWorld().spawnEntity(
                target.getLocation(),
                EntityType.COW);
        berryListener.onDamage(damage(attacker, cow, 1.0));

        assertFalse(items.isEdible(target.getInventory().getItemInMainHand()));
    }

    @Test
    void convertedItemStillConsumesAfterTransferToOffhand() {
        target.getInventory().setItemInMainHand(new ItemStack(Material.SHIELD, 2));
        berryListener.onDamage(damage(attacker, target, 1.0));
        ItemStack converted = target.getInventory().getItemInMainHand();
        target.getInventory().setItemInMainHand(null);
        target.getInventory().setItemInOffHand(converted);
        PlayerInteractEvent event = interact(target, EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_AIR);

        interactionListener.onInteract(event);

        assertTrue(event.isCancelled());
        assertEquals(1, target.getInventory().getItemInOffHand().getAmount());
        assertTrue(items.isEdible(target.getInventory().getItemInOffHand()));
    }

    @Test
    void pairedHandEventsConsumeOnlyOneItemPerServerTick() {
        target.getInventory().setItemInMainHand(edibles.convert(new ItemStack(Material.STONE)));
        target.getInventory().setItemInOffHand(edibles.convert(new ItemStack(Material.SHIELD, 2)));

        interactionListener.onInteract(interact(target, EquipmentSlot.HAND, Action.RIGHT_CLICK_AIR));
        interactionListener.onInteract(interact(target, EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_AIR));

        assertTrue(target.getInventory().getItemInMainHand().getType().isAir());
        assertEquals(2, target.getInventory().getItemInOffHand().getAmount());

        server.getScheduler().performTicks(1);
        interactionListener.onInteract(interact(target, EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_AIR));
        assertEquals(1, target.getInventory().getItemInOffHand().getAmount());
    }

    @Test
    void leftClickDoesNotConsumeConvertedItem() {
        target.getInventory().setItemInMainHand(edibles.convert(new ItemStack(Material.STONE, 2)));

        interactionListener.onInteract(interact(target, EquipmentSlot.HAND, Action.LEFT_CLICK_AIR));

        assertEquals(2, target.getInventory().getItemInMainHand().getAmount());
    }

    @Test
    void delayedRepeatedClicksQueueOnlyOneConsumption() {
        settings.set(new EdibleSettings(0, 0.0f, 2));
        target.getInventory().setItemInMainHand(edibles.convert(new ItemStack(Material.STONE, 3)));

        interactionListener.onInteract(interact(target, EquipmentSlot.HAND, Action.RIGHT_CLICK_AIR));
        interactionListener.onInteract(interact(target, EquipmentSlot.HAND, Action.RIGHT_CLICK_AIR));
        assertEquals(3, target.getInventory().getItemInMainHand().getAmount());

        server.getScheduler().performTicks(2);

        assertEquals(2, target.getInventory().getItemInMainHand().getAmount());
    }

    private static EntityDamageByEntityEvent damage(
            PlayerMock damager,
            LivingEntity victim,
            double amount) {
        return new EntityDamageByEntityEvent(damager, victim, DamageCause.ENTITY_ATTACK, amount);
    }

    private static PlayerInteractEvent interact(
            PlayerMock player,
            EquipmentSlot slot,
            Action action) {
        ItemStack held = slot == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        return new PlayerInteractEvent(player, action, held, null, null, slot);
    }

    private static YamlConfiguration defaultYaml() {
        InputStream stream = HungryBerryListenerTest.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new IllegalStateException("Bundled config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
