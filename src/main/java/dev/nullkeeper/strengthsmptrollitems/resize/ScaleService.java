package dev.nullkeeper.strengthsmptrollitems.resize;

import dev.nullkeeper.strengthsmptrollitems.items.PersistentKeys;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

public final class ScaleService {
    public static final double VANILLA_MINIMUM = 0.0625;
    public static final double VANILLA_MAXIMUM = 16.0;

    private final PersistentKeys keys;
    private final Attribute scaleAttribute;
    private final Consumer<String> warningSink;

    public ScaleService(PersistentKeys keys) {
        this(keys, Attribute.SCALE, ignored -> {});
    }

    public ScaleService(PersistentKeys keys, Consumer<String> warningSink) {
        this(keys, Attribute.SCALE, warningSink);
    }

    ScaleService(PersistentKeys keys, Attribute scaleAttribute) {
        this(keys, scaleAttribute, ignored -> {});
    }

    ScaleService(
            PersistentKeys keys,
            Attribute scaleAttribute,
            Consumer<String> warningSink) {
        this.keys = Objects.requireNonNull(keys, "keys");
        this.scaleAttribute = Objects.requireNonNull(scaleAttribute, "scaleAttribute");
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
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
        var data = entity.getPersistentDataContainer();
        Double stored = data.get(
                keys.scale(),
                PersistentDataType.DOUBLE);
        AttributeInstance scale = entity.getAttribute(scaleAttribute);
        if (stored == null) {
            if (data.has(keys.scale())) {
                warningSink.accept("Invalid stored scale type for living entity "
                        + entity.getUniqueId());
                data.remove(keys.scale());
            }
            return false;
        }
        if (scale == null) {
            return false;
        }
        if (!Double.isFinite(stored)) {
            warningSink.accept("Invalid stored scale for living entity " + entity.getUniqueId());
            data.remove(keys.scale());
            return false;
        }
        if (stored < VANILLA_MINIMUM || stored > VANILLA_MAXIMUM) {
            warningSink.accept("Out-of-range stored scale clamped for living entity "
                    + entity.getUniqueId());
        }
        scale.setBaseValue(Math.clamp(stored, VANILLA_MINIMUM, VANILLA_MAXIMUM));
        return true;
    }

    public record ScaleResult(boolean applied, double value) {}
}
