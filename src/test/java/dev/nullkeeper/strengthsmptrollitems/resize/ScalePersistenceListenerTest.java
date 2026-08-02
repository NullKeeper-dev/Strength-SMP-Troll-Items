package dev.nullkeeper.strengthsmptrollitems.resize;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerJoinEvent;
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

@SuppressWarnings({"deprecation", "removal"})
class ScalePersistenceListenerTest {
    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock player;
    private ScaleService scales;
    private ScalePersistenceListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        player = server.addPlayer("Target");
        scales = new ScaleService(new PersistentKeys(plugin), Attribute.MOVEMENT_SPEED);
        listener = new ScalePersistenceListener(plugin, scales);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void joinRestoresStoredScaleImmediately() {
        storeThenReset(player);

        listener.onJoin(new PlayerJoinEvent(player, ""));

        assertEquals(1.05, player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue());
    }

    @Test
    void respawnRestoresStoredScaleOneTickLater() {
        storeThenReset(player);

        listener.onRespawn(new PlayerRespawnEvent(player, player.getLocation(), false));
        assertEquals(1.0, player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue());

        server.getScheduler().performTicks(1);

        assertEquals(1.05, player.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue());
    }

    @Test
    void chunkLoadRestoresEveryStoredLivingEntity() {
        LivingEntity cow = (LivingEntity) player.getWorld().spawnEntity(
                player.getLocation(),
                EntityType.COW);
        storeThenReset(cow);

        listener.onChunkLoad(new ChunkLoadEvent(player.getLocation().getChunk(), false));

        assertEquals(1.05, cow.getAttribute(Attribute.MOVEMENT_SPEED).getBaseValue());
    }

    private void storeThenReset(LivingEntity entity) {
        entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(1.0);
        scales.apply(entity, false, 0.05);
        entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(1.0);
    }
}
