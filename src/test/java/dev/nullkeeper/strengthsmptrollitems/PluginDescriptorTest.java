package dev.nullkeeper.strengthsmptrollitems;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PluginDescriptorTest {
    @Test
    void descriptorDeclaresExpandedRuntimeContract() {
        YamlConfiguration descriptor = descriptor();

        assertEquals("StrengthSmpTrollItems", descriptor.getString("name"));
        assertEquals(StrengthSmpTrollItemsPlugin.class.getName(), descriptor.getString("main"));
        assertEquals("0.1.0", descriptor.getString("version"));
        assertEquals("26.1", descriptor.getString("api-version"));
        assertEquals(List.of("ProtocolLib"), descriptor.getStringList("depend"));
        assertNotNull(descriptor.getConfigurationSection("commands.trollitems"));
        assertEquals("op", descriptor.getString("permissions.trollitems.give.default"));
        assertEquals("op", descriptor.getString("permissions.trollitems.reload.default"));
    }

    private static YamlConfiguration descriptor() {
        InputStream stream = PluginDescriptorTest.class.getClassLoader().getResourceAsStream("plugin.yml");
        if (stream == null) {
            throw new IllegalStateException("Generated plugin.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
