package org.kynosarges.tektosyne.geometry;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Provides unit tests for class {@link RegularPolygon}.
 * @author Christoph Nahr
 * @version 6.0.0
 */
public class RegularPolygonTest {

    private final RegularPolygon
            hexagonEdge = new RegularPolygon(10.0, 6, PolygonOrientation.ON_EDGE),
            hexagonVertex = new RegularPolygon(10.0, 6, PolygonOrientation.ON_VERTEX),
            squareEdge = new RegularPolygon(10.0, 4, PolygonOrientation.ON_EDGE, true);

    @Test
    public void testHasTopIndex() {
        assertTrue(hexagonEdge.hasTopIndex);
        assertFalse(hexagonVertex.hasTopIndex);
        assertTrue(squareEdge.hasTopIndex);

        RegularPolygon polygon = new RegularPolygon(10.0, 5, PolygonOrientation.ON_EDGE);
        assertFalse(polygon.hasTopIndex);

        polygon = new RegularPolygon(10.0, 5, PolygonOrientation.ON_VERTEX);
        assertTrue(polygon.hasTopIndex);
    }

    @Test
    public void testAngleToIndex() {
        assertEquals(0, hexagonEdge.angleToIndex(-90.0));
        assertEquals(1, hexagonEdge.angleToIndex(-30.0));
        assertEquals(2, hexagonEdge.angleToIndex(30.0));
        assertEquals(3, hexagonEdge.angleToIndex(90.0));
        assertEquals(4, hexagonEdge.angleToIndex(150.0));
        assertEquals(5, hexagonEdge.angleToIndex(210.0));

        assertEquals(0, hexagonVertex.angleToIndex(-60.0));
        assertEquals(1, hexagonVertex.angleToIndex(0.0));
        assertEquals(2, hexagonVertex.angleToIndex(60.0));
        assertEquals(3, hexagonVertex.angleToIndex(120.0));
        assertEquals(4, hexagonVertex.angleToIndex(180.0));
        assertEquals(5, hexagonVertex.angleToIndex(240.0));

        assertEquals(1, squareEdge.angleToIndex(-23.0));
        assertEquals(2, squareEdge.angleToIndex(-22.0));
        assertEquals(3, squareEdge.angleToIndex(67.0));
        assertEquals(4, squareEdge.angleToIndex(68.0));
        assertEquals(5, squareEdge.angleToIndex(157.0));
        assertEquals(6, squareEdge.angleToIndex(158.0));
        assertEquals(7, squareEdge.angleToIndex(247.0));
        assertEquals(0, squareEdge.angleToIndex(248.0));
    }

    @Test
    public void testCompassToIndex() {
        assertEquals(0, hexagonEdge.compassToIndex(Compass.NORTH));
        assertEquals(1, hexagonEdge.compassToIndex(Compass.NORTH_EAST));
        assertEquals(2, hexagonEdge.compassToIndex(Compass.SOUTH_EAST));
        assertEquals(3, hexagonEdge.compassToIndex(Compass.SOUTH));
        assertEquals(4, hexagonEdge.compassToIndex(Compass.SOUTH_WEST));
        assertEquals(5, hexagonEdge.compassToIndex(Compass.NORTH_WEST));

        assertEquals(0, hexagonVertex.compassToIndex(Compass.NORTH_EAST));
        assertEquals(1, hexagonVertex.compassToIndex(Compass.EAST));
        assertEquals(2, hexagonVertex.compassToIndex(Compass.SOUTH_EAST));
        assertEquals(3, hexagonVertex.compassToIndex(Compass.SOUTH_WEST));
        assertEquals(4, hexagonVertex.compassToIndex(Compass.WEST));
        assertEquals(5, hexagonVertex.compassToIndex(Compass.NORTH_WEST));

        assertEquals(0, squareEdge.compassToIndex(Compass.NORTH));
        assertEquals(1, squareEdge.compassToIndex(Compass.NORTH_EAST));
        assertEquals(2, squareEdge.compassToIndex(Compass.EAST));
        assertEquals(3, squareEdge.compassToIndex(Compass.SOUTH_EAST));
        assertEquals(4, squareEdge.compassToIndex(Compass.SOUTH));
        assertEquals(5, squareEdge.compassToIndex(Compass.SOUTH_WEST));
        assertEquals(6, squareEdge.compassToIndex(Compass.WEST));
        assertEquals(7, squareEdge.compassToIndex(Compass.NORTH_WEST));
    }

    @Test
    public void testIndexToAngle() {
        assertEquals(30.0, hexagonEdge.indexToAngle(2), 0);
        assertEquals(90.0, hexagonEdge.indexToAngle(3), 0);
        assertEquals(150.0, hexagonEdge.indexToAngle(4), 0);
        assertEquals(210.0, hexagonEdge.indexToAngle(5), 0);
        assertEquals(270.0, hexagonEdge.indexToAngle(6), 0);
        assertEquals(330.0, hexagonEdge.indexToAngle(7), 0);

        assertEquals(60.0, hexagonVertex.indexToAngle(2), 0);
        assertEquals(120.0, hexagonVertex.indexToAngle(3), 0);
        assertEquals(180.0, hexagonVertex.indexToAngle(4), 0);
        assertEquals(240.0, hexagonVertex.indexToAngle(5), 0);
        assertEquals(300.0, hexagonVertex.indexToAngle(6), 0);
        assertEquals(0.0, hexagonVertex.indexToAngle(7), 0);

        assertEquals(0.0, squareEdge.indexToAngle(2), 0);
        assertEquals(45.0, squareEdge.indexToAngle(3), 0);
        assertEquals(90.0, squareEdge.indexToAngle(4), 0);
        assertEquals(135.0, squareEdge.indexToAngle(-3), 0);
        assertEquals(180.0, squareEdge.indexToAngle(-2), 0);
        assertEquals(225.0, squareEdge.indexToAngle(-1), 0);
        assertEquals(270.0, squareEdge.indexToAngle(0), 0);
        assertEquals(315.0, squareEdge.indexToAngle(1), 0);
    }

    @Test
    public void testIndexToCompass() {
        assertEquals(Compass.SOUTH_EAST, hexagonEdge.indexToCompass(2));
        assertEquals(Compass.SOUTH, hexagonEdge.indexToCompass(3));
        assertEquals(Compass.SOUTH_WEST, hexagonEdge.indexToCompass(4));
        assertEquals(Compass.NORTH_WEST, hexagonEdge.indexToCompass(5));
        assertEquals(Compass.NORTH, hexagonEdge.indexToCompass(6));
        assertEquals(Compass.NORTH_EAST, hexagonEdge.indexToCompass(7));

        assertEquals(Compass.SOUTH_EAST, hexagonVertex.indexToCompass(2));
        assertEquals(Compass.SOUTH_WEST, hexagonVertex.indexToCompass(3));
        assertEquals(Compass.WEST, hexagonVertex.indexToCompass(4));
        assertEquals(Compass.NORTH_WEST, hexagonVertex.indexToCompass(5));
        assertEquals(Compass.NORTH_EAST, hexagonVertex.indexToCompass(6));
        assertEquals(Compass.EAST, hexagonVertex.indexToCompass(7));

        assertEquals(Compass.EAST, squareEdge.indexToCompass(2));
        assertEquals(Compass.SOUTH_EAST, squareEdge.indexToCompass(3));
        assertEquals(Compass.SOUTH, squareEdge.indexToCompass(4));
        assertEquals(Compass.SOUTH_WEST, squareEdge.indexToCompass(-3));
        assertEquals(Compass.WEST, squareEdge.indexToCompass(-2));
        assertEquals(Compass.NORTH_WEST, squareEdge.indexToCompass(-1));
        assertEquals(Compass.NORTH, squareEdge.indexToCompass(0));
        assertEquals(Compass.NORTH_EAST, squareEdge.indexToCompass(1));
    }
}
