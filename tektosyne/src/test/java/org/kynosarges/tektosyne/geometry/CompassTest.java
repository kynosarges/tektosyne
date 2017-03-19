package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for class {@link Compass}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class CompassTest {

    @Test
    public void testFromDegrees() {
        for (int i = 0; i < 45; i += 5) {
            assertEquals(Compass.NORTH, Compass.fromDegrees(0 + i));
            assertEquals(Compass.NORTH_EAST, Compass.fromDegrees(45 + i));
            assertEquals(Compass.EAST, Compass.fromDegrees(90 + i));
            assertEquals(Compass.SOUTH_EAST, Compass.fromDegrees(135 + i));
            assertEquals(Compass.SOUTH, Compass.fromDegrees(180 + i));
            assertEquals(Compass.SOUTH_WEST, Compass.fromDegrees(225 + i));
            assertEquals(Compass.WEST, Compass.fromDegrees(270 + i));
            assertEquals(Compass.NORTH_WEST, Compass.fromDegrees(315 + i));
        }

        try {
            Compass.fromDegrees(-1);
            fail("expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) { }

        try {
            Compass.fromDegrees(360);
            fail("expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) { }
    }
}
