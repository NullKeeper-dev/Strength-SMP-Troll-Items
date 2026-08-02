package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
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
class SpookyCrossbowListenerTest {
    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock shooter;
    private PlayerMock target;
    private TrollItemService items;
    private ProjectileHitTracker tracker;
    private RavagerMetadataStore metadata;
    private PrivateRavagerRegistry registry;
    private PluginConfig config;
    private SpookyCrossbowListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        shooter = server.addPlayer("Shooter");
        target = server.addPlayer("Target");
        Location spawn = new Location(shooter.getWorld(), 0.5, 64.0, 0.5);
        shooter.teleport(spawn);
        target.teleport(spawn.clone().add(2.0, 0.0, 0.0));
        spawn.getChunk().load();
        prepareFloor(spawn, 8);

        PersistentKeys keys = new PersistentKeys(plugin);
        items = new TrollItemService(keys);
        tracker = new ProjectileHitTracker(keys);
        metadata = new RavagerMetadataStore(keys);
        registry = new PrivateRavagerRegistry();
        config = new ConfigLoader().load(defaultYaml());
        RavagerSpawner spawner = new RavagerSpawner(
                plugin,
                metadata,
                registry,
                ravager -> {},
                (location, settings) -> location.getWorld().spawn(location, Ravager.class));
        listener = new SpookyCrossbowListener(items, tracker, spawner, () -> config);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void markedCrossbowTagsProjectileWithShooterButUnmarkedDoesNot() {
        Arrow marked = shoot(shooter, items.create(TrollItemType.SPOOKY_CROSSBOW, config));
        Arrow ordinary = shoot(shooter, new ItemStack(Material.CROSSBOW));

        assertEquals(shooter.getUniqueId(), tracker.shooterId(marked).orElseThrow());
        assertFalse(tracker.shooterId(ordinary).isPresent());
    }

    @Test
    void everyDistinctSuccessfulHitAddsFivePersistentAssignedRavagers() {
        Arrow first = shoot(shooter, items.create(TrollItemType.SPOOKY_CROSSBOW, config));
        listener.onDamage(damage(first, target, 1.0));
        listener.onDamage(damage(first, target, 1.0));
        assertEquals(5, registry.snapshot().size());

        Arrow second = shoot(shooter, items.create(TrollItemType.SPOOKY_CROSSBOW, config));
        listener.onDamage(damage(second, target, 1.0));

        assertEquals(10, registry.snapshot().size());
        RavagerAssignment expected = new RavagerAssignment(
                shooter.getUniqueId(),
                target.getUniqueId());
        for (Ravager ravager : registry.snapshot().values()) {
            assertEquals(expected, metadata.read(ravager).orElseThrow());
        }
    }

    @Test
    void cancelledZeroDamageAndNonPlayerVictimsDoNotSpawn() {
        Arrow arrow = shoot(shooter, items.create(TrollItemType.SPOOKY_CROSSBOW, config));
        EntityDamageByEntityEvent cancelled = damage(arrow, target, 1.0);
        cancelled.setCancelled(true);
        listener.onDamage(cancelled);
        listener.onDamage(damage(arrow, target, 0.0));
        LivingEntity cow = (LivingEntity) target.getWorld().spawnEntity(
                target.getLocation(),
                EntityType.COW);
        listener.onDamage(damage(arrow, cow, 1.0));

        assertTrue(registry.snapshot().isEmpty());
    }

    @Test
    void assignmentsUseTheShooterStoredOnEachProjectile() {
        PlayerMock secondShooter = server.addPlayer("SecondShooter");
        secondShooter.teleport(shooter.getLocation());
        Arrow arrow = shoot(
                secondShooter,
                items.create(TrollItemType.SPOOKY_CROSSBOW, config));

        listener.onDamage(damage(arrow, target, 1.0));

        UUID actual = metadata.read(registry.snapshot().values().iterator().next())
                .orElseThrow()
                .shooterId();
        assertEquals(secondShooter.getUniqueId(), actual);
    }

    @Test
    void failedGroundSearchReportsPartialSpawn() {
        target.teleport(new Location(target.getWorld(), 0.5, 100.0, 0.5));
        Arrow arrow = shoot(shooter, items.create(TrollItemType.SPOOKY_CROSSBOW, config));

        listener.onDamage(damage(arrow, target, 1.0));

        assertEquals(0, registry.snapshot().size());
        assertEquals(
                ChatColor.YELLOW + "Only 0 of 5 Ravagers could be summoned.",
                shooter.nextMessage());
    }

    @Test
    void targetControllerPursuesOnlyAnEligibleAssignedPlayer() {
        Arrow arrow = shoot(shooter, items.create(TrollItemType.SPOOKY_CROSSBOW, config));
        listener.onDamage(damage(arrow, target, 1.0));
        Ravager ravager = registry.snapshot().values().iterator().next();
        RavagerTargetController controller = new RavagerTargetController(
                plugin,
                registry,
                metadata,
                ignored -> 32.0);

        assertTrue(target.isOnline());
        assertTrue(ravager.isValid());
        assertEquals(target.getWorld(), ravager.getWorld());
        controller.run();
        assertEquals(target, ravager.getTarget());

        target.teleport(server.addSimpleWorld("elsewhere").getSpawnLocation());
        controller.run();

        assertEquals(null, ravager.getTarget());
        assertEquals(target.getUniqueId(), metadata.read(ravager).orElseThrow().targetId());
    }

    private Arrow shoot(PlayerMock player, ItemStack bow) {
        Arrow arrow = player.getWorld().spawn(player.getEyeLocation(), Arrow.class);
        listener.onShoot(new EntityShootBowEvent(player, bow, arrow, 1.0f));
        return arrow;
    }

    private static EntityDamageByEntityEvent damage(
            Arrow arrow,
            LivingEntity victim,
            double amount) {
        return new EntityDamageByEntityEvent(arrow, victim, DamageCause.PROJECTILE, amount);
    }

    private static void prepareFloor(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                center.getWorld().getBlockAt(x, 63, z).setType(Material.STONE);
                center.getWorld().getBlockAt(x, 64, z).setType(Material.AIR);
                center.getWorld().getBlockAt(x, 65, z).setType(Material.AIR);
            }
        }
    }

    private static YamlConfiguration defaultYaml() {
        InputStream stream = SpookyCrossbowListenerTest.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new IllegalStateException("Bundled config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
