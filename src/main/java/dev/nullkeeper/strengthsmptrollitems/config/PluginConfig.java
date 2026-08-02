package dev.nullkeeper.strengthsmptrollitems.config;

import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PluginConfig(
        Map<TrollItemType, ItemPresentation> items,
        ResizeSettings resize,
        RavagerSettings ravagers,
        EdibleSettings edible,
        UpdateCheckerSettings updateChecker,
        Messages messages) {

    public PluginConfig {
        items = Map.copyOf(items);
        resize = Objects.requireNonNull(resize, "resize");
        ravagers = Objects.requireNonNull(ravagers, "ravagers");
        edible = Objects.requireNonNull(edible, "edible");
        updateChecker = Objects.requireNonNull(updateChecker, "updateChecker");
        messages = Objects.requireNonNull(messages, "messages");
    }

    public record ItemPresentation(String name, List<String> lore) {
        public ItemPresentation {
            name = Objects.requireNonNull(name, "name");
            lore = List.copyOf(lore);
        }
    }

    public record ResizeSettings(double step) {}

    public record RavagerSettings(
            int perHit,
            int speedLevel,
            double spawnRadius,
            int retargetIntervalTicks) {}

    public record EdibleSettings(
            int nutrition,
            float saturation,
            int consumeDelayTicks) {}

    public record UpdateCheckerSettings(boolean enabled) {}

    public record Messages(
            String noPermission,
            String usage,
            String giveUsage,
            String reloadUsage,
            String playerNotFound,
            String unknownItem,
            String invalidAmount,
            String giveSuccess,
            String giveReceived,
            String inventoryOverflow,
            String reloadSuccess,
            String reloadFailure,
            String internalError,
            String resizeSuccess,
            String unsupportedTarget,
            String emptyHand,
            String partialRavagerSpawn,
            String updateAvailable,
            String updateDisableHint) {

        public Messages {
            Objects.requireNonNull(noPermission, "noPermission");
            Objects.requireNonNull(usage, "usage");
            Objects.requireNonNull(giveUsage, "giveUsage");
            Objects.requireNonNull(reloadUsage, "reloadUsage");
            Objects.requireNonNull(playerNotFound, "playerNotFound");
            Objects.requireNonNull(unknownItem, "unknownItem");
            Objects.requireNonNull(invalidAmount, "invalidAmount");
            Objects.requireNonNull(giveSuccess, "giveSuccess");
            Objects.requireNonNull(giveReceived, "giveReceived");
            Objects.requireNonNull(inventoryOverflow, "inventoryOverflow");
            Objects.requireNonNull(reloadSuccess, "reloadSuccess");
            Objects.requireNonNull(reloadFailure, "reloadFailure");
            Objects.requireNonNull(internalError, "internalError");
            Objects.requireNonNull(resizeSuccess, "resizeSuccess");
            Objects.requireNonNull(unsupportedTarget, "unsupportedTarget");
            Objects.requireNonNull(emptyHand, "emptyHand");
            Objects.requireNonNull(partialRavagerSpawn, "partialRavagerSpawn");
            Objects.requireNonNull(updateAvailable, "updateAvailable");
            Objects.requireNonNull(updateDisableHint, "updateDisableHint");
        }
    }
}
