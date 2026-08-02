package dev.nullkeeper.strengthsmptrollitems.items;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class PersistentKeys {
    private final NamespacedKey itemType;
    private final NamespacedKey scale;
    private final NamespacedKey edible;
    private final NamespacedKey ravagerShooter;
    private final NamespacedKey ravagerTarget;
    private final NamespacedKey projectileShooter;
    private final NamespacedKey projectileVictims;

    public PersistentKeys(Plugin plugin) {
        this.itemType = new NamespacedKey(plugin, "item_type");
        this.scale = new NamespacedKey(plugin, "scale");
        this.edible = new NamespacedKey(plugin, "edible");
        this.ravagerShooter = new NamespacedKey(plugin, "ravager_shooter");
        this.ravagerTarget = new NamespacedKey(plugin, "ravager_target");
        this.projectileShooter = new NamespacedKey(plugin, "projectile_shooter");
        this.projectileVictims = new NamespacedKey(plugin, "projectile_victims");
    }

    public NamespacedKey itemType() {
        return itemType;
    }

    public NamespacedKey scale() {
        return scale;
    }

    public NamespacedKey edible() {
        return edible;
    }

    public NamespacedKey ravagerShooter() {
        return ravagerShooter;
    }

    public NamespacedKey ravagerTarget() {
        return ravagerTarget;
    }

    public NamespacedKey projectileShooter() {
        return projectileShooter;
    }

    public NamespacedKey projectileVictims() {
        return projectileVictims;
    }
}
