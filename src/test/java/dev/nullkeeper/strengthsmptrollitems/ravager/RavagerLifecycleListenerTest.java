package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.ArrayList;
import org.bukkit.entity.Ravager;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
class RavagerLifecycleListenerTest {
    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock shooter;
    private PlayerMock target;
    private PlayerMock outsider;
    private Ravager marked;
    private Ravager ordinary;
    private RavagerAssignment assignment;
    private RavagerMetadataStore metadata;
    private PrivateRavagerRegistry registry;
    private RavagerLifecycleListener lifecycle;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        shooter = server.addPlayer("Shooter");
        target = server.addPlayer("Target");
        outsider = server.addPlayer("Outsider");
        marked = shooter.getWorld().spawn(shooter.getLocation(), Ravager.class);
        ordinary = shooter.getWorld().spawn(shooter.getLocation(), Ravager.class);
        assignment = new RavagerAssignment(shooter.getUniqueId(), target.getUniqueId());
        metadata = new RavagerMetadataStore(new PersistentKeys(plugin));
        metadata.write(marked, assignment);
        registry = new PrivateRavagerRegistry();
        RavagerVisibilityService visibility = new RavagerVisibilityService(
                plugin,
                registry,
                metadata,
                new RavagerAccessPolicy());
        lifecycle = new RavagerLifecycleListener(plugin, registry, metadata, visibility);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void startupScanRegistersOnlyMarkedRavagers() {
        lifecycle.scanLoadedWorlds();

        assertTrue(registry.find(marked.getUniqueId()).isPresent());
        assertFalse(registry.find(ordinary.getUniqueId()).isPresent());
        assertFalse(outsider.canSee(marked));
    }

    @Test
    void chunkLoadRecoversMarkedRavagers() {
        lifecycle.onChunkLoad(new ChunkLoadEvent(marked.getLocation().getChunk(), false));

        assertTrue(registry.find(marked.getUniqueId()).isPresent());
        assertFalse(registry.find(ordinary.getUniqueId()).isPresent());
    }

    @Test
    void joinWorldChangeAndRespawnRefreshVisibilityWithoutChangingAssignment() {
        registry.register(marked);
        lifecycle.onJoin(new PlayerJoinEvent(outsider, ""));
        assertFalse(outsider.canSee(marked));

        lifecycle.onWorldChange(new PlayerChangedWorldEvent(outsider, outsider.getWorld()));
        assertFalse(outsider.canSee(marked));

        lifecycle.onRespawn(new PlayerRespawnEvent(target, target.getLocation(), false));
        server.getScheduler().performTicks(1);

        assertEquals(assignment, metadata.read(marked).orElseThrow());
    }

    @Test
    void targetLogoutClearsOnlyLiveTargetAndPreservesPersistentAssignment() {
        registry.register(marked);
        marked.setTarget(target);

        lifecycle.onQuit(new PlayerQuitEvent(target, ""));

        assertNull(marked.getTarget());
        assertEquals(assignment, metadata.read(marked).orElseThrow());
        assertTrue(registry.find(marked.getUniqueId()).isPresent());
    }

    @Test
    void ravagerDeathUnregistersWithoutRespawning() {
        registry.register(marked);

        lifecycle.onDeath(new EntityDeathEvent(marked, null, new ArrayList<>()));

        assertFalse(registry.find(marked.getUniqueId()).isPresent());
        assertEquals(2, shooter.getWorld().getEntitiesByClass(Ravager.class).size());
    }
}
