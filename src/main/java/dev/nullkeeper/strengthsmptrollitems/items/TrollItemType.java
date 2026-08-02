package dev.nullkeeper.strengthsmptrollitems.items;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;

public enum TrollItemType {
    RESIZING_SWORD("resizing_sword", "resizing-sword", Material.WOODEN_SWORD),
    SPOOKY_CROSSBOW("spooky_crossbow", "spooky-crossbow", Material.CROSSBOW),
    HUNGRY_BERRY("hungry_berry", "hungry-berry", Material.GLOW_BERRIES);

    private final String id;
    private final String configKey;
    private final Material material;

    TrollItemType(String id, String configKey, Material material) {
        this.id = id;
        this.configKey = configKey;
        this.material = material;
    }

    public String id() {
        return id;
    }

    public String configKey() {
        return configKey;
    }

    public Material material() {
        return material;
    }

    public static Optional<TrollItemType> fromId(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
                .filter(type -> type.id.equals(normalized))
                .findFirst();
    }
}
