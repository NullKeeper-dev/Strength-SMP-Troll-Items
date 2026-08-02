package dev.nullkeeper.strengthsmptrollitems;

import dev.nullkeeper.strengthsmptrollitems.combat.DamageTickPolicy;
import dev.nullkeeper.strengthsmptrollitems.command.GiveItemService;
import dev.nullkeeper.strengthsmptrollitems.command.TrollItemsCommand;
import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.ConfigService;
import dev.nullkeeper.strengthsmptrollitems.hungry.EdibleInteractionListener;
import dev.nullkeeper.strengthsmptrollitems.hungry.EdibleItemService;
import dev.nullkeeper.strengthsmptrollitems.hungry.HungryBerryListener;
import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.ravager.PrivateRavagerRegistry;
import dev.nullkeeper.strengthsmptrollitems.ravager.ProjectileHitTracker;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerAccessPolicy;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerLifecycleListener;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerMetadataStore;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerProtectionListener;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerSpawner;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerTargetController;
import dev.nullkeeper.strengthsmptrollitems.ravager.RavagerVisibilityService;
import dev.nullkeeper.strengthsmptrollitems.ravager.SpookyCrossbowListener;
import dev.nullkeeper.strengthsmptrollitems.resize.ResizingSwordListener;
import dev.nullkeeper.strengthsmptrollitems.resize.ScalePersistenceListener;
import dev.nullkeeper.strengthsmptrollitems.resize.ScaleService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RuntimeComponents implements AutoCloseable {
    private final JavaPlugin plugin;
    private final BukkitTask targetTask;
    private final AtomicBoolean closed = new AtomicBoolean();

    private RuntimeComponents(JavaPlugin plugin, BukkitTask targetTask) {
        this.plugin = plugin;
        this.targetTask = targetTask;
    }

    public static RuntimeComponents start(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        BukkitTask targetTask = null;
        try {
            ConfigService configs = configs(plugin);
            PersistentKeys keys = new PersistentKeys(plugin);
            TrollItemService items = new TrollItemService(keys);
            PrivateRavagerRegistry registry = new PrivateRavagerRegistry();
            RavagerMetadataStore metadata = new RavagerMetadataStore(
                    keys,
                    warning -> plugin.getLogger().warning(warning));
            RavagerAccessPolicy policy = new RavagerAccessPolicy();
            RavagerVisibilityService visibility = new RavagerVisibilityService(
                    plugin,
                    registry,
                    metadata,
                    policy);

            registerCommand(plugin, configs, items);
            RavagerTargetController controller = registerListeners(
                    plugin,
                    configs,
                    keys,
                    items,
                    registry,
                    metadata,
                    policy,
                    visibility);
            targetTask = plugin.getServer().getScheduler().runTaskTimer(
                    plugin,
                    new DynamicTargetTask(plugin, configs, controller),
                    1L,
                    1L);
            return new RuntimeComponents(plugin, targetTask);
        } catch (RuntimeException exception) {
            cleanupFailedStart(plugin, targetTask);
            throw exception;
        }
    }

    private static ConfigService configs(JavaPlugin plugin) {
        ConfigLoader loader = new ConfigLoader(message -> plugin.getLogger().warning(message));
        return new ConfigService(loader, plugin.getConfig(), () -> {
            plugin.reloadConfig();
            return plugin.getConfig();
        });
    }

    private static void registerCommand(
            JavaPlugin plugin,
            ConfigService configs,
            TrollItemService items) {
        TrollItemsCommand handler = new TrollItemsCommand(
                configs,
                new GiveItemService(items),
                plugin.getLogger());
        PluginCommand command = Objects.requireNonNull(
                plugin.getCommand("trollitems"),
                "plugin.yml is missing the trollitems command");
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    private static RavagerTargetController registerListeners(
            JavaPlugin plugin,
            ConfigService configs,
            PersistentKeys keys,
            TrollItemService items,
            PrivateRavagerRegistry registry,
            RavagerMetadataStore metadata,
            RavagerAccessPolicy policy,
            RavagerVisibilityService visibility) {
        ScaleService scales = new ScaleService(
                keys,
                warning -> plugin.getLogger().warning(warning));
        DamageTickPolicy damageTicks = new DamageTickPolicy();
        ScalePersistenceListener scalePersistence = new ScalePersistenceListener(plugin, scales);
        EdibleItemService edibles = new EdibleItemService(
                items,
                () -> configs.current().edible());
        RavagerSpawner spawner = new RavagerSpawner(
                plugin,
                metadata,
                registry,
                visibility::refresh);
        RavagerLifecycleListener lifecycle = new RavagerLifecycleListener(
                plugin,
                registry,
                metadata,
                visibility);
        List<Listener> listeners = List.of(
                new ResizingSwordListener(items, scales, damageTicks, configs::current),
                scalePersistence,
                new HungryBerryListener(items, edibles, damageTicks, configs::current),
                new EdibleInteractionListener(
                        plugin,
                        items,
                        edibles,
                        () -> configs.current().edible()),
                new SpookyCrossbowListener(
                        items,
                        new ProjectileHitTracker(
                                keys,
                                warning -> plugin.getLogger().warning(warning)),
                        spawner,
                        damageTicks,
                        configs::current),
                new RavagerProtectionListener(metadata, policy),
                lifecycle);
        PluginManager manager = plugin.getServer().getPluginManager();
        listeners.forEach(listener -> manager.registerEvents(listener, plugin));
        scalePersistence.scanLoadedWorlds();
        lifecycle.scanLoadedWorlds();
        return new RavagerTargetController(plugin, registry, metadata);
    }

    private static void cleanupFailedStart(
            JavaPlugin plugin,
            BukkitTask targetTask) {
        if (targetTask != null) {
            targetTask.cancel();
        }
        HandlerList.unregisterAll(plugin);
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        targetTask.cancel();
        HandlerList.unregisterAll(plugin);
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }

    private static final class DynamicTargetTask implements Runnable {
        private final JavaPlugin plugin;
        private final ConfigService configs;
        private final RavagerTargetController controller;
        private final AtomicInteger elapsed = new AtomicInteger();

        private DynamicTargetTask(
                JavaPlugin plugin,
                ConfigService configs,
                RavagerTargetController controller) {
            this.plugin = plugin;
            this.configs = configs;
            this.controller = controller;
        }

        @Override
        public void run() {
            try {
                int interval = configs.current().ravagers().retargetIntervalTicks();
                if (elapsed.incrementAndGet() >= interval) {
                    elapsed.set(0);
                    controller.run();
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Private Ravager target task failed", exception);
            }
        }
    }
}
