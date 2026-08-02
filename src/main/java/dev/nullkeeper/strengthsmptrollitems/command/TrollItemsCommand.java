package dev.nullkeeper.strengthsmptrollitems.command;

import dev.nullkeeper.strengthsmptrollitems.command.GiveItemService.GiveResult;
import dev.nullkeeper.strengthsmptrollitems.config.ConfigService;
import dev.nullkeeper.strengthsmptrollitems.config.ConfigService.ReloadResult;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import dev.nullkeeper.strengthsmptrollitems.text.LegacyText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TrollItemsCommand implements CommandExecutor, TabCompleter {
    private static final String GIVE_PERMISSION = "trollitems.give";
    private static final String RELOAD_PERMISSION = "trollitems.reload";

    private final ConfigService configs;
    private final GiveItemService giver;
    private final Logger logger;
    private final Runnable successfulReloadHook;

    public TrollItemsCommand(
            ConfigService configs,
            GiveItemService giver,
            Logger logger,
            Runnable successfulReloadHook) {
        this.configs = Objects.requireNonNull(configs, "configs");
        this.giver = Objects.requireNonNull(giver, "giver");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.successfulReloadHook = Objects.requireNonNull(
                successfulReloadHook,
                "successfulReloadHook");
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        try {
            if (args.length == 0) {
                sendAvailableUsage(sender);
                return true;
            }
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give" -> executeGive(sender, args);
                case "reload" -> executeReload(sender, args);
                default -> {
                    sendAvailableUsage(sender);
                    yield true;
                }
            };
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, "Failed /trollitems command", exception);
            send(sender, configs.current().messages().internalError());
            return true;
        }
    }

    private void sendAvailableUsage(CommandSender sender) {
        PluginConfig.Messages messages = configs.current().messages();
        boolean sent = false;
        if (sender.hasPermission(GIVE_PERMISSION)) {
            send(sender, messages.giveUsage());
            sent = true;
        }
        if (sender.hasPermission(RELOAD_PERMISSION)) {
            send(sender, messages.reloadUsage());
            sent = true;
        }
        if (!sent) {
            send(sender, messages.noPermission());
        }
    }

    private boolean executeGive(CommandSender sender, String[] args) {
        PluginConfig config = configs.current();
        if (!sender.hasPermission(GIVE_PERMISSION)) {
            send(sender, config.messages().noPermission());
            return true;
        }
        if (args.length < 3 || args.length > 4) {
            send(sender, config.messages().usage());
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            send(sender, format(config.messages().playerNotFound(), "player", args[1]));
            return true;
        }
        TrollItemType type = TrollItemType.fromId(args[2]).orElse(null);
        if (type == null) {
            send(sender, format(config.messages().unknownItem(), "item", args[2]));
            return true;
        }
        Integer amount = parseAmount(args);
        if (amount == null) {
            send(sender, config.messages().invalidAmount());
            return true;
        }

        GiveResult result = giver.give(target, type, amount, config);
        Map<String, Object> values = Map.of(
                "amount", amount,
                "item", type.id(),
                "player", target.getName());
        send(sender, LegacyText.format(config.messages().giveSuccess(), values));
        send(target, LegacyText.format(config.messages().giveReceived(), values));
        if (result.dropped() > 0) {
            send(sender, LegacyText.format(
                    config.messages().inventoryOverflow(),
                    Map.of("amount", result.dropped(), "player", target.getName())));
        }
        return true;
    }

    private boolean executeReload(CommandSender sender, String[] args) {
        PluginConfig config = configs.current();
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            send(sender, config.messages().noPermission());
            return true;
        }
        if (args.length != 1) {
            send(sender, config.messages().usage());
            return true;
        }
        ReloadResult result = configs.reloadFromDisk();
        if (result.successful()) {
            successfulReloadHook.run();
            send(sender, configs.current().messages().reloadSuccess());
        } else {
            logger.warning("Configuration reload rejected: " + result.message());
            send(sender, config.messages().reloadFailure());
        }
        return true;
    }

    private static Integer parseAmount(String[] args) {
        if (args.length == 3) {
            return 1;
        }
        try {
            int amount = Integer.parseInt(args[3]);
            return amount >= 1 && amount <= 64 ? amount : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String format(String template, String key, Object value) {
        return LegacyText.format(template, Map.of(key, value));
    }

    private static void send(CommandSender target, String message) {
        target.sendMessage(LegacyText.color(message));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length == 1) {
            List<String> choices = new ArrayList<>(2);
            if (sender.hasPermission(GIVE_PERMISSION)) {
                choices.add("give");
            }
            if (sender.hasPermission(RELOAD_PERMISSION)) {
                choices.add("reload");
            }
            return filterPrefix(choices, args[0]);
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return filterPrefix(List.of("resizing_sword", "spooky_crossbow", "hungry_berry"), args[2]);
        }
        return List.of();
    }

    private static List<String> filterPrefix(List<String> choices, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return choices.stream()
                .filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(normalized))
                .sorted()
                .toList();
    }
}
