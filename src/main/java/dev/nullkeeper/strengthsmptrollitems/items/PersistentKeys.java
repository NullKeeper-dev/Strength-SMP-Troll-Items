package dev.nullkeeper.strengthsmptrollitems.items;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PersistentKeys {
    private final NamespacedKey itemType;
    private final NamespacedKey scale;

    public PersistentKeys(Plugin plugin) {
        this.itemType = new NamespacedKey(plugin, "item_type");
        this.scale = new NamespacedKey(plugin, "scale");
    }

    public NamespacedKey itemType() {
        return itemType;
    }

    public NamespacedKey scale() {
        return scale;
    }
}
