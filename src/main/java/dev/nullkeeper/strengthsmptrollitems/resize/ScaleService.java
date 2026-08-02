package dev.nullkeeper.strengthsmptrollitems.resize;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.Objects;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

public final class ScaleService {
    public static final double VANILLA_MINIMUM = 0.0625;
    public static final double VANILLA_MAXIMUM = 16.0;

    private final PersistentKeys keys;
    private final Attribute scaleAttribute;

    public ScaleService(PersistentKeys keys) {
        this(keys, Attribute.SCALE);
    }

    ScaleService(PersistentKeys keys, Attribute scaleAttribute) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.scaleAttribute = Objects.requireNonNull(scaleAttribute, "scaleAttribute");
    }

    public ScaleResult apply(LivingEntity entity, boolean shrink, double step) {
        AttributeInstance scale = entity.getAttribute(scaleAttribute);
        if (scale == null) {
            return new ScaleResult(false, Double.NaN);
        }
        double changed = ScaleMath.change(
                scale.getBaseValue(),
                step,
                shrink,
                VANILLA_MINIMUM,
                VANILLA_MAXIMUM);
        scale.setBaseValue(changed);
        entity.getPersistentDataContainer().set(
                keys.scale(),
                PersistentDataType.DOUBLE,
                changed);
        return new ScaleResult(true, changed);
    }

    public boolean restore(LivingEntity entity) {
        Double stored = entity.getPersistentDataContainer().get(
                keys.scale(),
                PersistentDataType.DOUBLE);
        AttributeInstance scale = entity.getAttribute(scaleAttribute);
        if (stored == null || !Double.isFinite(stored) || scale == null) {
            return false;
        }
        scale.setBaseValue(Math.clamp(stored, VANILLA_MINIMUM, VANILLA_MAXIMUM));
        return true;
    }

    public record ScaleResult(boolean applied, double value) {}
}
