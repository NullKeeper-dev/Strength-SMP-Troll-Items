package dev.nullkeeper.strengthsmptrollitems.resize;

import dev.nullkeeper.strengthsmptrollitems.combat.DamageTickPolicy;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemService;
import dev.nullkeeper.strengthsmptrollitems.items.TrollItemType;
import dev.nullkeeper.strengthsmptrollitems.resize.ScaleService.ScaleResult;
import dev.nullkeeper.strengthsmptrollitems.text.LegacyText;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class ResizingSwordListener implements Listener {
    private final TrollItemService items;
    private final ScaleService scales;
    private final DamageTickPolicy damageTicks;
    private final Supplier<PluginConfig> configSource;

    public ResizingSwordListener(
            TrollItemService items,
            ScaleService scales,
            DamageTickPolicy damageTicks,
            Supplier<PluginConfig> configSource) {
        this.items = Objects.requireNonNull(items, "items");
        this.scales = Objects.requireNonNull(scales, "scales");
        this.damageTicks = Objects.requireNonNull(damageTicks, "damageTicks");
        this.configSource = Objects.requireNonNull(configSource, "configSource");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)
                || !(event.getEntity() instanceof LivingEntity target)
                || !damageTicks.qualifies(event, target)
                || !items.isType(
                        attacker.getInventory().getItemInMainHand(),
                        TrollItemType.RESIZING_SWORD)) {
            return;
        }

        PluginConfig config = configSource.get();
        ScaleResult result = scales.apply(target, attacker.isSneaking(), config.resize().step());
        if (!result.applied()) {
            attacker.sendMessage(LegacyText.color(config.messages().unsupportedTarget()));
            return;
        }
        attacker.sendMessage(LegacyText.format(
                config.messages().resizeSuccess(),
                Map.of("target", target.getName(), "size", ScaleMath.format(result.value()))));
    }
}
