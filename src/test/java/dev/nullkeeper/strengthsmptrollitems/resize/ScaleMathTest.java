package dev.nullkeeper.strengthsmptrollitems.resize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScaleMathTest {
    @Test
    void growsByApprovedDefaultStep() {
        assertEquals(1.05, ScaleMath.change(1.0, 0.05, false, 0.0625, 16.0));
    }

    @Test
    void shrinksByApprovedDefaultStep() {
        assertEquals(0.95, ScaleMath.change(1.0, 0.05, true, 0.0625, 16.0));
    }

    @Test
    void clampsAtVanillaBounds() {
        assertEquals(16.0, ScaleMath.change(15.99, 0.05, false, 0.0625, 16.0));
        assertEquals(0.0625, ScaleMath.change(0.07, 0.05, true, 0.0625, 16.0));
    }

    @Test
    void formatsConfiguredPrecisionWithoutFloatingPointNoise() {
        assertEquals("1.05", ScaleMath.format(1.0500000000000003));
        assertEquals("0.0625", ScaleMath.format(0.0625));
        assertEquals("16", ScaleMath.format(16.0));
    }

    @Test
    void rejectsNonFiniteOrInvalidArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> ScaleMath.change(Double.NaN, 0.05, false, 0.0625, 16.0));
        assertThrows(IllegalArgumentException.class,
                () -> ScaleMath.change(1.0, -0.05, false, 0.0625, 16.0));
        assertThrows(IllegalArgumentException.class,
                () -> ScaleMath.change(1.0, 0.05, false, 16.0, 0.0625));
    }
}
