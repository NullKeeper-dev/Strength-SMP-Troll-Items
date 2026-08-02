package dev.nullkeeper.strengthsmptrollitems.config;

import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {
    private final ConfigLoader loader = new ConfigLoader();

    @Test
    void loadsApprovedDefaultsFromBundledYaml() {
        PluginConfig config = loader.load(defaultYaml());

        assertEquals(0.05, config.resize().step());
        assertEquals(5, config.ravagers().perHit());
        assertEquals(2, config.ravagers().speedLevel());
        assertEquals(6.0, config.ravagers().spawnRadius());
        assertEquals(20, config.ravagers().retargetIntervalTicks());
        assertEquals(0, config.edible().nutrition());
        assertEquals(0.0F, config.edible().saturation());
        assertEquals(0, config.edible().consumeDelayTicks());
        assertEquals("&e&lResizing Sword", config.items().get(TrollItemType.RESIZING_SWORD).name());
        assertEquals(2, config.items().get(TrollItemType.HUNGRY_BERRY).lore().size());
        assertEquals("&e{target}'s size is now {size}", config.messages().resizeSuccess());
    }

    @Test
    void missingKeysUseDocumentedDefaults() {
        PluginConfig config = loader.load(new YamlConfiguration());

        assertEquals(0.05, config.resize().step());
        assertEquals(5, config.ravagers().perHit());
        assertEquals(0, config.edible().nutrition());
        assertEquals("&5&lSpooky Crossbow", config.items().get(TrollItemType.SPOOKY_CROSSBOW).name());
    }

    @ParameterizedTest(name = "rejects {0}={1}")
    @MethodSource("invalidNumericSettings")
    void rejectsOutOfRangeNumericSettings(String path, Object value) {
        YamlConfiguration yaml = defaultYaml();
        yaml.set(path, value);

        assertThrows(ConfigException.class, () -> loader.load(yaml));
    }

    @Test
    void failedReloadKeepsSameValidSnapshot() {
        YamlConfiguration invalid = defaultYaml();
        invalid.set("ravagers.per-hit", 0);
        ConfigService service = new ConfigService(loader, defaultYaml(), () -> invalid);
        PluginConfig before = service.current();

        ConfigService.ReloadResult result = service.reload(invalid);

        assertFalse(result.successful());
        assertSame(before, service.current());
    }

    @Test
    void unknownKeysAreIgnoredWithAWarning() {
        List<String> warnings = new ArrayList<>();
        YamlConfiguration yaml = defaultYaml();
        yaml.set("ravagers.unlimited-chaos", true);

        new ConfigLoader(warnings::add).load(yaml);

        assertEquals(List.of("Unknown config key: ravagers.unlimited-chaos"), warnings);
    }

    private static Stream<Arguments> invalidNumericSettings() {
        return Stream.of(
                Arguments.of("resize.step", -0.01),
                Arguments.of("resize.step", Double.NaN),
                Arguments.of("ravagers.per-hit", 0),
                Arguments.of("ravagers.per-hit", 65),
                Arguments.of("ravagers.speed-level", 0),
                Arguments.of("ravagers.speed-level", 256),
                Arguments.of("ravagers.spawn-radius", 0.0),
                Arguments.of("ravagers.spawn-radius", 65.0),
                Arguments.of("ravagers.retarget-interval-ticks", 0),
                Arguments.of("edible.nutrition", -1),
                Arguments.of("edible.nutrition", 21),
                Arguments.of("edible.saturation", -0.01),
                Arguments.of("edible.consume-delay-ticks", -1),
                Arguments.of("edible.consume-delay-ticks", 72001));
    }

    private static YamlConfiguration defaultYaml() {
        InputStream stream = ConfigLoaderTest.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new IllegalStateException("Bundled config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
