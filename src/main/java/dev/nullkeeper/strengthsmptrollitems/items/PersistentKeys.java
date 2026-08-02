package dev.nullkeeper.strengthsmptrollitems.items;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PersistentKeys {
    private final NamespacedKey itemType;

    public PersistentKeys(Plugin plugin) {
        this.itemType = new NamespacedKey(plugin, "item_type");
    }

    public NamespacedKey itemType() {
        return itemType;
    }
}
