package dev.nullkeeper.strengthsmptrollitems.ravager;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketListener;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolRavagerIsolationTest {
    private PluginMock plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "test");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void startRegistersBothFiltersAndCloseRemovesOwnedListeners() {
        List<PacketListener> listeners = new ArrayList<>();
        AtomicBoolean removed = new AtomicBoolean();
        ProtocolManager manager = (ProtocolManager) Proxy.newProxyInstance(
                ProtocolManager.class.getClassLoader(),
                new Class<?>[] {ProtocolManager.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("addPacketListener")) {
                        listeners.add((PacketListener) arguments[0]);
                        return null;
                    }
                    if (method.getName().equals("removePacketListeners")) {
                        removed.set(arguments[0] == plugin);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
        PersistentKeys keys = new PersistentKeys(plugin);
        ProtocolRavagerIsolation isolation = new ProtocolRavagerIsolation(
                plugin,
                manager,
                new PrivateRavagerRegistry(),
                new RavagerMetadataStore(keys),
                new RavagerAccessPolicy());

        isolation.start();
        assertEquals(2, listeners.size());

        isolation.close();
        assertTrue(removed.get());
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
