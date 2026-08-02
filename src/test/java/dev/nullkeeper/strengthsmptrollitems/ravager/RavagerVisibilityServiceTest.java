package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.RavagerSettings;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Material;
import org.bukkit.entity.Ravager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RavagerVisibilityServiceTest {
    private PluginMock plugin;
    private PlayerMock shooter;
    private PlayerMock target;
    private PlayerMock outsider;
    private Ravager ravager;
    private RavagerMetadataStore metadata;
    private PrivateRavagerRegistry registry;
    private RavagerVisibilityService visibility;
    private Set<UUID> hiddenByDefault;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "test");
        shooter = server.addPlayer("Shooter");
        target = server.addPlayer("Target");
        outsider = server.addPlayer("Outsider");
        ravager = shooter.getWorld().spawn(shooter.getLocation(), Ravager.class);
        PersistentKeys keys = new PersistentKeys(plugin);
        metadata = new RavagerMetadataStore(keys);
        metadata.write(ravager, new RavagerAssignment(
                shooter.getUniqueId(),
                target.getUniqueId()));
        registry = new PrivateRavagerRegistry();
        registry.register(ravager);
        hiddenByDefault = new HashSet<>();
        visibility = new RavagerVisibilityService(
                plugin,
                registry,
                metadata,
                new RavagerAccessPolicy(),
                candidate -> hiddenByDefault.add(candidate.getUniqueId()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void refreshShowsParticipantsAndHidesOutsiders() {
        visibility.refresh(ravager);

        assertTrue(hiddenByDefault.contains(ravager.getUniqueId()));
        assertTrue(shooter.canSee(ravager));
        assertTrue(target.canSee(ravager));
        assertFalse(outsider.canSee(ravager));
    }

    @Test
    void refreshingJoiningPlayerAppliesEveryRegisteredAssignment() {
        visibility.refresh(outsider);

        assertFalse(outsider.canSee(ravager));
    }

    @Test
    void unmarkedRavagerIsNeverHidden() {
        Ravager ordinary = shooter.getWorld().spawn(shooter.getLocation(), Ravager.class);

        visibility.refresh(ordinary);

        assertTrue(outsider.canSee(ordinary));
    }

    @Test
    void newlySpawnedPrivateRavagerIsHiddenImmediately() {
        shooter.getLocation().getChunk().load();
        target.teleport(shooter.getLocation());
        shooter.getWorld().getBlockAt(
                shooter.getLocation().getBlockX(),
                shooter.getLocation().getBlockY() - 1,
                shooter.getLocation().getBlockZ()).setType(Material.STONE);
        AtomicReference<RavagerSettings> initializedSettings = new AtomicReference<>();
        RavagerSpawner spawner = new RavagerSpawner(
                plugin,
                metadata,
                registry,
                visibility::refresh,
                (candidate, settings) -> {
                    hiddenByDefault.add(candidate.getUniqueId());
                    initializedSettings.set(settings);
                },
                (location, initializer) -> {
                    Ravager spawned = location.getWorld().spawn(location, Ravager.class);
                    initializer.accept(spawned);
                    return spawned;
                });

        spawner.spawn(shooter.getUniqueId(), target, new RavagerSettings(1, 2, 1.0, 20));

        Ravager spawned = registry.snapshot().values().stream()
                .filter(candidate -> candidate != ravager)
                .findFirst()
                .orElseThrow();
        assertTrue(hiddenByDefault.contains(spawned.getUniqueId()));
        assertEquals(new RavagerSettings(1, 2, 1.0, 20), initializedSettings.get());
        assertFalse(outsider.canSee(spawned));
    }
}
