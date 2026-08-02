package dev.nullkeeper.strengthsmptrollitems.resize;

import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"deprecation", "removal"})
class ResizingSwordListenerTest {
    private ServerMock server;
    private PlayerMock attacker;
    private PlayerMock target;
    private Attribute scaleAttribute;
    private PersistentKeys keys;
    private TrollItemService items;
    private ScaleService scales;
    private PluginConfig config;
    private ResizingSwordListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        PluginMock plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "0.1.0");
        attacker = server.addPlayer("Attacker");
        target = server.addPlayer("Target");
        scaleAttribute = Attribute.MOVEMENT_SPEED;
        target.getAttribute(scaleAttribute).setBaseValue(1.0);
        keys = new PersistentKeys(plugin);
        items = new TrollItemService(keys);
        scales = new ScaleService(keys, scaleAttribute);
        config = new ConfigLoader().load(defaultYaml());
        listener = new ResizingSwordListener(items, scales, () -> config);
        attacker.getInventory().setItemInMainHand(items.create(TrollItemType.RESIZING_SWORD, config));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void successfulStandingHitGrowsAndReportsPlayerScale() {
        listener.onDamage(damage(attacker, target, 1.0));

        assertEquals(1.05, target.getAttribute(scaleAttribute).getBaseValue());
        assertEquals(ChatColor.YELLOW + "Target's size is now 1.05", attacker.nextMessage());
    }

    @Test
    void successfulSneakingHitShrinksAnyLivingEntity() {
        attacker.setSneaking(true);
        LivingEntity cow = (LivingEntity) target.getWorld().spawnEntity(
                target.getLocation(),
                EntityType.COW);
        cow.getAttribute(scaleAttribute).setBaseValue(1.0);

        listener.onDamage(damage(attacker, cow, 1.0));

        assertEquals(0.95, cow.getAttribute(scaleAttribute).getBaseValue());
    }

    @Test
    void cancelledOrZeroDamageHitDoesNothing() {
        EntityDamageByEntityEvent cancelled = damage(attacker, target, 1.0);
        cancelled.setCancelled(true);
        listener.onDamage(cancelled);
        listener.onDamage(damage(attacker, target, 0.0));

        assertEquals(1.0, target.getAttribute(scaleAttribute).getBaseValue());
    }

    @Test
    void unmarkedMainHandItemDoesNothing() {
        attacker.getInventory().setItemInMainHand(null);

        listener.onDamage(damage(attacker, target, 1.0));

        assertEquals(1.0, target.getAttribute(scaleAttribute).getBaseValue());
    }

    @Test
    void targetWithoutConfiguredAttributeReportsUnsupported() {
        LivingEntity cow = (LivingEntity) target.getWorld().spawnEntity(
                target.getLocation(),
                EntityType.COW);
        ResizingSwordListener unsupportedListener = new ResizingSwordListener(
                items,
                new ScaleService(keys, Attribute.FLYING_SPEED),
                () -> config);

        unsupportedListener.onDamage(damage(attacker, cow, 1.0));

        assertEquals(
                ChatColor.RED + "That living entity does not expose Minecraft's scale attribute.",
                attacker.nextMessage());
    }

    @Test
    void storedScaleRestoresAfterAttributeReset() {
        ScaleService.ScaleResult applied = scales.apply(target, false, 0.05);
        target.getAttribute(scaleAttribute).setBaseValue(1.0);

        boolean restored = scales.restore(target);

        assertTrue(applied.applied());
        assertTrue(restored);
        assertEquals(1.05, target.getAttribute(scaleAttribute).getBaseValue());
    }

    private static EntityDamageByEntityEvent damage(
            PlayerMock damager,
            LivingEntity victim,
            double amount) {
        return new EntityDamageByEntityEvent(damager, victim, DamageCause.ENTITY_ATTACK, amount);
    }

    private static YamlConfiguration defaultYaml() {
        InputStream stream = ResizingSwordListenerTest.class.getClassLoader().getResourceAsStream("config.yml");
        if (stream == null) {
            throw new IllegalStateException("Bundled config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
