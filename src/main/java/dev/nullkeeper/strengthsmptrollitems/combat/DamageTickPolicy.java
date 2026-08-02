package dev.nullkeeper.strengthsmptrollitems.combat;

import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class DamageTickPolicy {
    public boolean qualifies(EntityDamageByEntityEvent event, LivingEntity target) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(target, "target");
        if (event.isCancelled()
                || event.getDamage() <= 0.0
                || target.isInvulnerable()) {
            return false;
        }
        if (target instanceof Player player) {
            GameMode mode = player.getGameMode();
            return mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR;
        }
        return true;
    }
}
