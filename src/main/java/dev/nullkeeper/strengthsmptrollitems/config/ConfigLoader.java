package dev.nullkeeper.strengthsmptrollitems.config;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.EdibleSettings;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.ItemPresentation;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.Messages;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.RavagerSettings;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.ResizeSettings;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.configuration.ConfigurationSection;

public final class ConfigLoader {
    private static final Map<TrollItemType, ItemPresentation> DEFAULT_ITEMS = Map.of(
            TrollItemType.RESIZING_SWORD,
            new ItemPresentation(
                    "&e&lResizing Sword",
                    List.of("&7Attack to grow target", "&7Sneak + Attack to shrink target")),
            TrollItemType.SPOOKY_CROSSBOW,
            new ItemPresentation(
                    "&5&lSpooky Crossbow",
                    List.of("&7Shoot a player to summon their fears.")),
            TrollItemType.HUNGRY_BERRY,
            new ItemPresentation(
                    "&#FFA500&lHungry Berry",
                    List.of("&7Do not eat.", "&7Hit a player to make their held item edible.")));
    private static final Set<String> KNOWN_PATHS = Set.of(
            "items",
            "items.resizing-sword",
            "items.resizing-sword.name",
            "items.resizing-sword.lore",
            "items.spooky-crossbow",
            "items.spooky-crossbow.name",
            "items.spooky-crossbow.lore",
            "items.hungry-berry",
            "items.hungry-berry.name",
            "items.hungry-berry.lore",
            "resize",
            "resize.step",
            "ravagers",
            "ravagers.per-hit",
            "ravagers.speed-level",
            "ravagers.spawn-radius",
            "ravagers.retarget-interval-ticks",
            "edible",
            "edible.nutrition",
            "edible.saturation",
            "edible.consume-delay-ticks",
            "messages",
            "messages.no-permission",
            "messages.usage",
            "messages.give-usage",
            "messages.reload-usage",
            "messages.player-not-found",
            "messages.unknown-item",
            "messages.invalid-amount",
            "messages.give-success",
            "messages.give-received",
            "messages.inventory-overflow",
            "messages.reload-success",
            "messages.reload-failure",
            "messages.internal-error",
            "messages.resize-success",
            "messages.unsupported-target",
            "messages.empty-hand",
            "messages.partial-ravager-spawn");
    private final Consumer<String> warningSink;

    public ConfigLoader() {
        this(ignored -> {});
    }

    public ConfigLoader(Consumer<String> warningSink) {
        this.warningSink = warningSink;
    }

    public PluginConfig load(ConfigurationSection root) {
        Map<TrollItemType, ItemPresentation> items = new EnumMap<>(TrollItemType.class);
        for (TrollItemType type : TrollItemType.values()) {
            ItemPresentation defaults = DEFAULT_ITEMS.get(type);
            String base = "items." + type.configKey();
            items.put(type, new ItemPresentation(
                    string(root, base + ".name", defaults.name()),
                    stringList(root, base + ".lore", defaults.lore())));
        }

        double resizeStep = decimal(root, "resize.step", 0.05);
        requireFiniteRange("resize.step", resizeStep, 0.0, Double.MAX_VALUE, true);

        int perHit = integer(root, "ravagers.per-hit", 5);
        requireRange("ravagers.per-hit", perHit, 1, 64);
        int speedLevel = integer(root, "ravagers.speed-level", 2);
        requireRange("ravagers.speed-level", speedLevel, 1, 255);
        double spawnRadius = decimal(root, "ravagers.spawn-radius", 6.0);
        requireFiniteRange("ravagers.spawn-radius", spawnRadius, 0.0, Double.MAX_VALUE, false);
        int retargetTicks = integer(root, "ravagers.retarget-interval-ticks", 20);
        requireRange("ravagers.retarget-interval-ticks", retargetTicks, 1, 1200);

        int nutrition = integer(root, "edible.nutrition", 0);
        requireRange("edible.nutrition", nutrition, 0, 20);
        double saturationValue = decimal(root, "edible.saturation", 0.0);
        requireFiniteRange("edible.saturation", saturationValue, 0.0, Float.MAX_VALUE, true);
        int consumeTicks = integer(root, "edible.consume-delay-ticks", 0);
        requireRange("edible.consume-delay-ticks", consumeTicks, 0, 72000);

        PluginConfig loaded = new PluginConfig(
                items,
                new ResizeSettings(resizeStep),
                new RavagerSettings(perHit, speedLevel, spawnRadius, retargetTicks),
                new EdibleSettings(nutrition, (float) saturationValue, consumeTicks),
                loadMessages(root));
        warnUnknownKeys(root);
        return loaded;
    }

    private void warnUnknownKeys(ConfigurationSection root) {
        root.getKeys(true).stream()
                .filter(path -> !KNOWN_PATHS.contains(path))
                .filter(ConfigLoader::hasKnownParent)
                .sorted()
                .forEach(path -> warningSink.accept("Unknown config key: " + path));
    }

    private static boolean hasKnownParent(String path) {
        int separator = path.lastIndexOf('.');
        return separator < 0 || KNOWN_PATHS.contains(path.substring(0, separator));
    }

    private static Messages loadMessages(ConfigurationSection root) {
        return new Messages(
                string(root, "messages.no-permission", "&cYou do not have permission to use this command."),
                string(root, "messages.usage", "&eUsage: /trollitems give <player> <resizing_sword|spooky_crossbow|hungry_berry> [amount] or /trollitems reload"),
                string(root, "messages.give-usage", "&eUsage: /trollitems give <player> <resizing_sword|spooky_crossbow|hungry_berry> [amount]"),
                string(root, "messages.reload-usage", "&eUsage: /trollitems reload"),
                string(root, "messages.player-not-found", "&cPlayer '{player}' is not online."),
                string(root, "messages.unknown-item", "&cUnknown troll item '{item}'."),
                string(root, "messages.invalid-amount", "&cAmount must be a whole number from 1 through 64."),
                string(root, "messages.give-success", "&aGave {amount} {item} to {player}."),
                string(root, "messages.give-received", "&aYou received {amount} {item}."),
                string(root, "messages.inventory-overflow", "&e{amount} item(s) did not fit and were dropped at {player}'s location."),
                string(root, "messages.reload-success", "&aStrength SMP Troll Items configuration reloaded."),
                string(root, "messages.reload-failure", "&cReload failed; the previous configuration is still active. Check the console."),
                string(root, "messages.internal-error", "&cThe command failed unexpectedly. Check the server console."),
                string(root, "messages.resize-success", "&e{target}'s size is now {size}"),
                string(root, "messages.unsupported-target", "&cThat living entity does not expose Minecraft's scale attribute."),
                string(root, "messages.empty-hand", "&e{target} is not holding an item in their main hand."),
                string(root, "messages.partial-ravager-spawn", "&eOnly {spawned} of {requested} Ravagers could be summoned."));
    }

    private static String string(ConfigurationSection root, String path, String defaultValue) {
        Object value = root.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String text)) {
            throw new ConfigException(path + " must be text");
        }
        return text;
    }

    private static List<String> stringList(
            ConfigurationSection root,
            String path,
            List<String> defaultValue) {
        Object value = root.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof List<?> values)
                || values.stream().anyMatch(element -> !(element instanceof String))) {
            throw new ConfigException(path + " must be a list of text values");
        }
        return values.stream().map(String.class::cast).toList();
    }

    private static int integer(ConfigurationSection root, String path, int defaultValue) {
        Object value = root.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new ConfigException(path + " must be a whole number");
        }
        double decimal = number.doubleValue();
        if (!Double.isFinite(decimal) || decimal != Math.rint(decimal)) {
            throw new ConfigException(path + " must be a whole number");
        }
        return number.intValue();
    }

    private static double decimal(ConfigurationSection root, String path, double defaultValue) {
        Object value = root.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new ConfigException(path + " must be a number");
        }
        return number.doubleValue();
    }

    private static void requireRange(String path, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new ConfigException(path + " must be from " + minimum + " through " + maximum);
        }
    }

    private static void requireFiniteRange(
            String path,
            double value,
            double minimum,
            double maximum,
            boolean includeMinimum) {
        boolean belowMinimum = includeMinimum ? value < minimum : value <= minimum;
        if (!Double.isFinite(value) || belowMinimum || value > maximum) {
            String comparison = includeMinimum ? "at least " : "greater than ";
            throw new ConfigException(path + " must be finite and " + comparison + minimum);
        }
    }
}

final class ConfigException extends IllegalArgumentException {
    ConfigException(String message) {
        super(message);
    }
}
