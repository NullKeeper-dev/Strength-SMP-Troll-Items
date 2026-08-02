package dev.nullkeeper.strengthsmptrollitems.combat;

import com.google.common.base.Function;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

@SuppressWarnings({"deprecation", "removal"})
public final class DamageEventFixtures {
    private DamageEventFixtures() {}

    public static EntityDamageByEntityEvent zeroFinalDamage(
            Entity damager,
            LivingEntity victim,
            DamageCause cause) {
        Map<DamageModifier, Double> modifiers = new EnumMap<>(DamageModifier.class);
        modifiers.put(DamageModifier.BASE, 1.0);
        modifiers.put(DamageModifier.ARMOR, -1.0);
        Map<DamageModifier, Function<? super Double, Double>> functions =
                new EnumMap<>(DamageModifier.class);
        functions.put(DamageModifier.BASE, damage -> damage);
        functions.put(DamageModifier.ARMOR, damage -> -damage);
        return new EntityDamageByEntityEvent(
                damager,
                victim,
                cause,
                modifiers,
                functions);
    }
}
