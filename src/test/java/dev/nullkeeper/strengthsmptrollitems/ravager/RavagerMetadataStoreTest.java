package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.Map;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Ravager;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RavagerMetadataStoreTest {
    private PluginMock plugin;
    private Ravager first;
    private Ravager second;
    private RavagerMetadataStore metadata;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        first = server.addSimpleWorld("world").spawn(
                server.getWorld("world").getSpawnLocation(),
                Ravager.class);
        second = server.getWorld("world").spawn(
                server.getWorld("world").getSpawnLocation(),
                Ravager.class);
        metadata = new RavagerMetadataStore(new PersistentKeys(plugin));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void missingOrMalformedMetadataReturnsEmpty() {
        assertFalse(metadata.read(first).isPresent());
        first.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "ravager_shooter"),
                PersistentDataType.STRING,
                "not-a-uuid");
        first.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "ravager_target"),
                PersistentDataType.STRING,
                UUID.randomUUID().toString());

        assertFalse(metadata.read(first).isPresent());
    }

    @Test
    void validMetadataSurvivesPersistentContainerCopy() {
        RavagerAssignment assignment = new RavagerAssignment(UUID.randomUUID(), UUID.randomUUID());
        metadata.write(first, assignment);
        first.getPersistentDataContainer().copyTo(second.getPersistentDataContainer(), true);

        assertEquals(assignment, metadata.read(second).orElseThrow());
    }

    @Test
    void registryUsesImmutableCopyOnWriteSnapshots() {
        PrivateRavagerRegistry registry = new PrivateRavagerRegistry();
        registry.register(first);
        Map<UUID, Ravager> snapshot = registry.snapshot();

        assertEquals(first, registry.find(first.getUniqueId()).orElseThrow());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put(second.getUniqueId(), second));

        registry.register(second);
        assertFalse(snapshot.containsKey(second.getUniqueId()));
        registry.unregister(first.getUniqueId());
        assertFalse(registry.find(first.getUniqueId()).isPresent());
    }
}
