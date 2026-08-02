package dev.nullkeeper.strengthsmptrollitems;

import dev.nullkeeper.strengthsmptrollitems.update.UpdateNotificationListener;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginIntegrationTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void enableComposesRuntimeAndDisableCleansOwnedResources() {
        StrengthSmpTrollItemsPlugin plugin = MockBukkit.load(StrengthSmpTrollItemsPlugin.class);

        assertTrue(plugin.isEnabled());
        assertEquals(0.05, plugin.getConfig().getDouble("resize.step"));
        assertNotNull(plugin.getCommand("trollitems"));
        assertNotNull(plugin.getCommand("trollitems").getExecutor());
        assertTrue(HandlerList.getRegisteredListeners(plugin).size() >= 9);
        assertTrue(HandlerList.getRegisteredListeners(plugin).stream()
                .anyMatch(listener -> listener.getListener() instanceof UpdateNotificationListener));
        assertTrue(server.getScheduler().getPendingTasks().stream()
                .anyMatch(task -> task.getOwner() == plugin));

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(plugin.isEnabled());
        assertEquals(0, HandlerList.getRegisteredListeners(plugin).size());
        assertFalse(server.getScheduler().getPendingTasks().stream()
                .map(BukkitTask::getOwner)
                .anyMatch(owner -> owner == plugin));
    }
}
