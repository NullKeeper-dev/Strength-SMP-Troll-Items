package dev.nullkeeper.strengthsmptrollitems;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketListener;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
        MockBukkit.createMockPlugin("ProtocolLib", "5.4.0");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void enableComposesRuntimeAndDisableCleansOwnedResources() {
        List<PacketListener> packetListeners = new ArrayList<>();
        AtomicBoolean protocolRemoved = new AtomicBoolean();
        ProtocolManager manager = protocolManager(packetListeners, protocolRemoved);

        StrengthSmpTrollItemsPlugin plugin = MockBukkit.load(
                StrengthSmpTrollItemsPlugin.class,
                manager);

        assertTrue(plugin.isEnabled());
        assertEquals(0.05, plugin.getConfig().getDouble("resize.step"));
        assertNotNull(plugin.getCommand("trollitems"));
        assertNotNull(plugin.getCommand("trollitems").getExecutor());
        assertTrue(HandlerList.getRegisteredListeners(plugin).size() >= 8);
        assertEquals(2, packetListeners.size());
        assertTrue(server.getScheduler().getPendingTasks().stream()
                .anyMatch(task -> task.getOwner() == plugin));

        server.getPluginManager().disablePlugin(plugin);

        assertFalse(plugin.isEnabled());
        assertTrue(protocolRemoved.get());
        assertEquals(0, HandlerList.getRegisteredListeners(plugin).size());
        assertFalse(server.getScheduler().getPendingTasks().stream()
                .map(BukkitTask::getOwner)
                .anyMatch(owner -> owner == plugin));
    }

    private static ProtocolManager protocolManager(
            List<PacketListener> listeners,
            AtomicBoolean removed) {
        return (ProtocolManager) Proxy.newProxyInstance(
                ProtocolManager.class.getClassLoader(),
                new Class<?>[] {ProtocolManager.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("addPacketListener")) {
                        listeners.add((PacketListener) arguments[0]);
                        return null;
                    }
                    if (method.getName().equals("removePacketListeners")) {
                        removed.set(true);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class || type == short.class || type == byte.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
