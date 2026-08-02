package dev.nullkeeper.strengthsmptrollitems.ravager;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig.RavagerSettings;
import org.bukkit.entity.Ravager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@FunctionalInterface
interface RavagerInitializer {
    void initialize(Ravager ravager, RavagerSettings settings);

    static RavagerInitializer paper() {
        return (ravager, settings) -> {
            ravager.setVisibleByDefault(false);
            ravager.setPersistent(true);
            ravager.setRemoveWhenFarAway(false);
            ravager.setCollidable(false);
            ravager.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    PotionEffect.INFINITE_DURATION,
                    settings.speedLevel() - 1,
                    false,
                    false));
        };
    }
}
