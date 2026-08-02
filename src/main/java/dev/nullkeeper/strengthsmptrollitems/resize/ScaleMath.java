package dev.nullkeeper.strengthsmptrollitems.resize;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ScaleMath {
    private static final int DISPLAY_SCALE = 6;

    private ScaleMath() {}

    public static double change(
            double current,
            double step,
            boolean shrink,
            double minimum,
            double maximum) {
        if (!Double.isFinite(current)
                || !Double.isFinite(step)
                || !Double.isFinite(minimum)
                || !Double.isFinite(maximum)
                || step < 0.0
                || minimum > maximum) {
            throw new IllegalArgumentException("Scale values must be finite with a valid nonnegative range");
        }
        double changed = current + (shrink ? -step : step);
        return Math.clamp(changed, minimum, maximum);
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Scale value must be finite");
        }
        return BigDecimal.valueOf(value)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
