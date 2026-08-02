package dev.nullkeeper.strengthsmptrollitems.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public final class LegacyText {
    private static final Pattern HEX_COLOR = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private LegacyText() {}

    public static String color(String input) {
        Matcher matcher = HEX_COLOR.matcher(input);
        StringBuilder expanded = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char character : hex.toCharArray()) {
                replacement.append('&').append(character);
            }
            matcher.appendReplacement(expanded, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(expanded);
        return ChatColor.translateAlternateColorCodes('&', expanded.toString());
    }

    public static String format(String template, Map<String, ?> values) {
        String formatted = template;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            formatted = formatted.replace('{' + entry.getKey() + '}', String.valueOf(entry.getValue()));
        }
        return color(formatted);
    }
}
