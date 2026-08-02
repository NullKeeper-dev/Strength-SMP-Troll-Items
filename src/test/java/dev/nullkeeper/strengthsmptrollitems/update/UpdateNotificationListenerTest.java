package dev.nullkeeper.strengthsmptrollitems.update;

import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
class UpdateNotificationListenerTest {
    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock admin;
    private PermissionAttachment permission;
    private AtomicReference<PluginConfig> config;
    private AtomicReference<Optional<UpdateInfo>> update;
    private UpdateNotificationListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("StrengthSmpTrollItems", "test");
        admin = server.addPlayer("Admin");
        admin.setOp(false);
        permission = admin.addAttachment(plugin, "trollitems.update-notify", true);
        config = new AtomicReference<>(config(true));
        update = new AtomicReference<>(Optional.of(new UpdateInfo(
                "2.0.0",
                "2.1.0",
                ModrinthApiClient.PROJECT_PAGE)));
        listener = new UpdateNotificationListener(config::get, update::get);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void eligibleAdminIsNotifiedOnlyOncePerRuntime() {
        listener.onJoin(new PlayerJoinEvent(admin, "joined"));
        listener.onJoin(new PlayerJoinEvent(admin, "joined again"));

        assertEquals(
                ChatColor.YELLOW
                        + "Strength SMP Troll Items 2.0.0 is outdated. Version 2.1.0 is available: "
                        + ModrinthApiClient.PROJECT_PAGE,
                admin.nextMessage());
        assertEquals(
                ChatColor.GRAY
                        + "Disable update alerts with update-checker.enabled: false in config.yml.",
                admin.nextMessage());
        admin.assertNoMoreSaid();
    }

    @Test
    void missingPermissionSuppressesNotice() {
        permission.setPermission("trollitems.update-notify", false);

        listener.onJoin(new PlayerJoinEvent(admin, "joined"));

        admin.assertNoMoreSaid();
    }

    @Test
    void disabledConfigurationSuppressesNotice() {
        config.set(config(false));

        listener.onJoin(new PlayerJoinEvent(admin, "joined"));

        admin.assertNoMoreSaid();
    }

    @Test
    void absentUpdateSuppressesNoticeWithoutUsingTheRuntimeSlot() {
        update.set(Optional.empty());
        listener.onJoin(new PlayerJoinEvent(admin, "joined"));
        update.set(Optional.of(new UpdateInfo("2.0.0", "2.1.0", ModrinthApiClient.PROJECT_PAGE)));

        listener.onJoin(new PlayerJoinEvent(admin, "joined later"));

        assertEquals(
                ChatColor.YELLOW
                        + "Strength SMP Troll Items 2.0.0 is outdated. Version 2.1.0 is available: "
                        + ModrinthApiClient.PROJECT_PAGE,
                admin.nextMessage());
    }

    private static PluginConfig config(boolean enabled) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("update-checker.enabled", enabled);
        return new ConfigLoader().load(yaml);
    }
}
