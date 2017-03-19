package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for class {@link Angle}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class AngleTest {

    private final double DELTA = 0.0001;

    @Test
    public void testDegreesToCompass() {
        assertEquals(Compass.NORTH, Angle.degreesToCompass(-22.0));
        assertEquals(Compass.NORTH, Angle.degreesToCompass(0.0));
        assertEquals(Compass.NORTH, Angle.degreesToCompass(22.0));

        assertEquals(Compass.NORTH_WEST, Angle.degreesToCompass(-23.0));
        assertEquals(Compass.NORTH_EAST, Angle.degreesToCompass(23.0));
    }

    @Test
    public void testDistanceDegrees() {
        for (double a = 0; a < 360; a++)
            for (double b = 0; b < 360; b++) {
                double dist = Angle.distanceDegrees(a, b);
                assertTrue(-180 < dist && dist <= 180);
                assertEquals(b, Angle.normalizeDegrees(a + dist), DELTA);
            }
    }

    @Test
    public void testDistanceRadians() {
        for (double a = 0; a < 2 * Math.PI; a += Angle.DEGREES_TO_RADIANS)
            for (double b = 0; b < 2 * Math.PI; b += Angle.DEGREES_TO_RADIANS) {
                double dist = Angle.distanceRadians(a, b);
                assertTrue(-Math.PI < dist && dist <= Math.PI);
                assertEquals(b, Angle.normalizeRadians(a + dist), Angle.DEGREES_TO_RADIANS / 2);
            }
    }

    @Test
    public void testNormalizeDegrees() {
        assertEquals(0, Angle.normalizeDegrees(0), DELTA);
        assertEquals(0.4, Angle.normalizeDegrees(0.4), DELTA);
        assertEquals(359.6, Angle.normalizeDegrees(-0.4), DELTA);

        assertEquals(0, Angle.normalizeDegrees(360), DELTA);
        assertEquals(0.4, Angle.normalizeDegrees(360.4), DELTA);
        assertEquals(359.4, Angle.normalizeDegrees(-0.6), DELTA);

        assertEquals(180, Angle.normalizeDegrees(180), DELTA);
        assertEquals(180, Angle.normalizeDegrees(540), DELTA);
        assertEquals(180, Angle.normalizeDegrees(-180), DELTA);
        assertEquals(180, Angle.normalizeDegrees(-540), DELTA);
    }

    @Test
    public void testNormalizeRoundedDegrees() {
        assertEquals(0, Angle.normalizeRoundedDegrees(0));
        assertEquals(0, Angle.normalizeRoundedDegrees(0.4));
        assertEquals(0, Angle.normalizeRoundedDegrees(-0.4));

        assertEquals(0, Angle.normalizeRoundedDegrees(360));
        assertEquals(0, Angle.normalizeRoundedDegrees(360.4));
        assertEquals(359, Angle.normalizeRoundedDegrees(-0.6));

        assertEquals(180, Angle.normalizeRoundedDegrees(180));
        assertEquals(180, Angle.normalizeRoundedDegrees(540));
        assertEquals(180, Angle.normalizeRoundedDegrees(-180));
        assertEquals(180, Angle.normalizeRoundedDegrees(-540));

        try {
            Angle.normalizeRoundedDegrees(1e100);
            fail("expected ArithmeticException");
        } catch (ArithmeticException e) { }
    }

    @Test
    public void testNormalizeRadians() {
        assertEquals(0, Angle.normalizeRadians(0), DELTA);
        assertEquals(0.4, Angle.normalizeRadians(0.4), DELTA);
        assertEquals(2 * Math.PI - 0.4, Angle.normalizeRadians(-0.4), DELTA);

        assertEquals(0, Angle.normalizeRadians(2 * Math.PI), DELTA);
        assertEquals(0.4, Angle.normalizeRadians(2 * Math.PI + 0.4), DELTA);
        assertEquals(2 * Math.PI - 0.6, Angle.normalizeRadians(-0.6), DELTA);

        assertEquals(Math.PI, Angle.normalizeRadians(Math.PI), DELTA);
        assertEquals(Math.PI, Angle.normalizeRadians(3 * Math.PI), DELTA);
        assertEquals(Math.PI, Angle.normalizeRadians(-Math.PI), DELTA);
        assertEquals(Math.PI, Angle.normalizeRadians(-3 * Math.PI), DELTA);
    }
}
