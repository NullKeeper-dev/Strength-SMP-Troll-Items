package dev.nullkeeper.strengthsmptrollitems.items;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.ItemPresentation;
import dev.nullkeeper.strengthsmptrollitems.text.LegacyText;
import java.util.Objects;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

@SuppressWarnings("deprecation")
public final class TrollItemService {
    private final PersistentKeys keys;

    public TrollItemService(PersistentKeys keys) {
        this.keys = Objects.requireNonNull(keys, "keys");
    }

    public ItemStack create(TrollItemType type, PluginConfig config) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        ItemPresentation presentation = config.items().get(type);
        meta.setDisplayName(LegacyText.color(presentation.name()));
        meta.setLore(presentation.lore().stream().map(LegacyText::color).toList());
        meta.getPersistentDataContainer().set(
                keys.itemType(),
                PersistentDataType.STRING,
                type.id());
        if (type == TrollItemType.RESIZING_SWORD) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean isType(ItemStack item, TrollItemType expected) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(
                keys.itemType(),
                PersistentDataType.STRING);
        return expected.id().equals(value);
    }
}
